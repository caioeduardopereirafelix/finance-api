package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.EmailNotVerifiedException;
import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidEmailVerificationToken;
import io.github.caioeduardopereirafelix.financeapi.exceptions.VerificationResendTooSoonException;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.model.entity.EmailVerificationToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.EmailVerificationTokenRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private EmailDispatcher emailDispatcher;

    @InjectMocks
    private EmailVerificationService service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expirationHours", 24L);
        ReflectionTestUtils.setField(service, "cooldownSeconds", 60L);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:4200/");
        user = User.builder().id(UUID.randomUUID()).name("Caio").email("caio@test.com").build();
    }

    private EmailVerificationToken stored(Instant createdAt, Instant expiresAt) {
        return EmailVerificationToken.builder()
                .id(UUID.randomUUID()).tokenHash("hash").user(user)
                .createdAt(createdAt).expiresAt(expiresAt).build();
    }

    @Test
    void envioInicialGuardaSoOHashEMandaOLinkNoFragmento() {
        service.sendInitial(user);

        ArgumentCaptor<EmailVerificationToken> saved = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(saved.capture());
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailDispatcher).dispatch(anyString(), anyString(), body.capture());

        String link = body.getValue().lines()
                .filter(l -> l.startsWith("http://localhost:4200/confirmar-email#token="))
                .findFirst().orElseThrow();
        String token = link.substring(link.indexOf('=') + 1);

        assertEquals(SecureTokens.sha256(token), saved.getValue().getTokenHash());
        assertTrue(saved.getValue().getExpiresAt().isAfter(Instant.now().plus(23, ChronoUnit.HOURS)));
        verify(tokenRepository).deleteByUser(user);
    }

    @Test
    void reenvioDentroDoIntervaloEhRecusadoComOTempoQueFalta() {
        when(tokenRepository.findFirstByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(stored(Instant.now().minusSeconds(10), Instant.now().plusSeconds(3600))));

        var e = assertThrows(VerificationResendTooSoonException.class, () -> service.resend(user));

        assertTrue(e.getRetryAfterSeconds() > 40 && e.getRetryAfterSeconds() <= 51);
        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailDispatcher);
    }

    @Test
    void reenvioDepoisDoIntervaloGeraNovoToken() {
        when(tokenRepository.findFirstByUserOrderByCreatedAtDesc(user))
                .thenReturn(Optional.of(stored(Instant.now().minusSeconds(120), Instant.now().plusSeconds(3600))));

        service.resend(user);

        verify(tokenRepository).save(any());
        verify(emailDispatcher).dispatch(anyString(), anyString(), anyString());
    }

    @Test
    void reenvioParaQuemJaConfirmouNaoFazNada() {
        user.setEmailVerifiedAt(Instant.now());

        service.resend(user);

        verifyNoInteractions(tokenRepository, emailDispatcher);
    }

    @Test
    void tokenDesconhecidoOuExpiradoOuJaConsumidoEhRecusado() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThrows(InvalidEmailVerificationToken.class, () -> service.verify("x"));

        var expired = stored(Instant.now().minusSeconds(100), Instant.now().minusSeconds(1));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));
        assertThrows(InvalidEmailVerificationToken.class, () -> service.verify("x"));

        var racing = stored(Instant.now(), Instant.now().plusSeconds(3600));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(racing));
        when(tokenRepository.consume(racing.getId())).thenReturn(0);
        assertThrows(InvalidEmailVerificationToken.class, () -> service.verify("x"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void confirmarMarcaOUsuarioELimpaOsTokens() {
        var token = stored(Instant.now(), Instant.now().plusSeconds(3600));
        when(tokenRepository.findByTokenHash(SecureTokens.sha256("abc"))).thenReturn(Optional.of(token));
        when(tokenRepository.consume(token.getId())).thenReturn(1);

        service.verify("abc");

        assertNotNull(user.getEmailVerifiedAt());
        verify(userRepository).save(user);
        verify(tokenRepository).deleteByUser(user);
    }

    @Test
    void exigirConfirmacaoBarraSoQuemNaoConfirmou() {
        assertThrows(EmailNotVerifiedException.class, () -> service.requireVerified(user));

        user.setEmailVerifiedAt(Instant.now());
        assertDoesNotThrow(() -> service.requireVerified(user));
    }
}

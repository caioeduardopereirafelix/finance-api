package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidPasswordResetToken;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.model.entity.PasswordResetToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.PasswordResetTokenRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private LoginAttemptService loginAttempts;
    @Mock
    private EmailDispatcher emailDispatcher;
    @Mock
    private AccountNotifications accountNotifications;

    @InjectMocks
    private PasswordResetService service;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expirationMinutes", 30L);
        ReflectionTestUtils.setField(service, "cooldownSeconds", 60L);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:4200/");
        user = User.builder().id(UUID.randomUUID()).name("Caio").email("caio@test.com").password("antiga").build();
    }

    private PasswordResetToken stored(Instant expiresAt) {
        return PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hash")
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    void emailDesconhecidoNaoGeraTokenNemEmail() {
        when(userRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

        service.requestReset("nao@existe.com");

        verifyNoInteractions(tokenRepository, emailDispatcher);
    }

    @Test
    void emailConhecidoGuardaSoOHashEMandaOLinkComOTokenNoFragmento() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.existsByUserAndCreatedAtAfter(any(), any())).thenReturn(false);

        service.requestReset(user.getEmail());

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailDispatcher).dispatch(anyString(), anyString(), body.capture());

        String link = body.getValue().lines()
                .filter(l -> l.startsWith("http://localhost:4200/redefinir-senha#token="))
                .findFirst().orElseThrow();
        String token = link.substring(link.indexOf('=') + 1);

        assertEquals(SecureTokens.sha256(token), saved.getValue().getTokenHash());
        assertFalse(body.getValue().contains(saved.getValue().getTokenHash()));
        assertTrue(saved.getValue().getExpiresAt().isAfter(Instant.now().plus(29, ChronoUnit.MINUTES)));
        assertTrue(saved.getValue().getExpiresAt().isBefore(Instant.now().plus(31, ChronoUnit.MINUTES)));
        verify(tokenRepository).deleteByUser(user);
    }

    @Test
    void pedidoDentroDoIntervaloNaoGeraOutroTokenNemEmail() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokenRepository.existsByUserAndCreatedAtAfter(any(), any())).thenReturn(true);

        service.requestReset(user.getEmail());

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailDispatcher);
    }

    @Test
    void tokenDesconhecidoEhRecusado() {
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidPasswordResetToken.class, () -> service.resetPassword("x", "nova-senha-1"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void tokenExpiradoEhRecusadoESemTrocarASenha() {
        when(tokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(stored(Instant.now().minus(1, ChronoUnit.MINUTES))));

        assertThrows(InvalidPasswordResetToken.class, () -> service.resetPassword("x", "nova-senha-1"));

        verify(tokenRepository, never()).consume(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void tokenJaConsumidoPorOutraRequisicaoEhRecusado() {
        var token = stored(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(tokenRepository.consume(token.getId())).thenReturn(0);

        assertThrows(InvalidPasswordResetToken.class, () -> service.resetPassword("x", "nova-senha-1"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void redefinirTrocaASenhaRevogaSessoesDestravaOLoginEAvisaPorEmail() {
        var token = stored(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(tokenRepository.findByTokenHash(SecureTokens.sha256("abc"))).thenReturn(Optional.of(token));
        when(tokenRepository.consume(token.getId())).thenReturn(1);
        when(passwordEncoder.encode("nova-senha-1")).thenReturn("hash-novo");

        service.resetPassword("abc", "nova-senha-1");

        assertEquals("hash-novo", user.getPassword());
        verify(userRepository).save(user);
        verify(tokenRepository).deleteByUser(user);
        verify(refreshTokenService).revokeAllFor(user);
        verify(loginAttempts).recordSuccess("caio@test.com");
        verify(accountNotifications).passwordChanged(user);
    }
}

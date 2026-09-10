package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidRefreshToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.RefreshToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.RefreshTokenRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "expirationTime", 604800000L);
        user = User.builder().id(UUID.randomUUID()).email("user@test.com").build();
    }

    private RefreshToken stored(Instant expiresAt, boolean revoked) {
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hash")
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .revoked(revoked)
                .build();
    }

    @Test
    void deveGerarTokenSemGuardarOValorEmClaro() {
        String token = refreshTokenService.generate(user);

        var captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertNotEquals(token, saved.getTokenHash());
        assertEquals(64, saved.getTokenHash().length());
        assertTrue(saved.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void deveRecusarTokenDesconhecido() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshToken.class, () -> refreshTokenService.consume("qualquer"));
    }

    @Test
    void deveRecusarTokenJaRevogado() {
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(stored(Instant.now().plus(1, ChronoUnit.DAYS), true)));

        assertThrows(InvalidRefreshToken.class, () -> refreshTokenService.consume("qualquer"));
    }

    @Test
    void deveRecusarTokenExpirado() {
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(stored(Instant.now().minus(1, ChronoUnit.DAYS), false)));

        assertThrows(InvalidRefreshToken.class, () -> refreshTokenService.consume("qualquer"));
    }

    @Test
    void deveRevogarOTokenAoConsumir() {
        RefreshToken valido = stored(Instant.now().plus(1, ChronoUnit.DAYS), false);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(valido));

        User resultado = refreshTokenService.consume("qualquer");

        assertEquals(user, resultado);
        assertTrue(valido.isRevoked(), "o token deve ser invalidado no consumo (rotacao)");
        verify(refreshTokenRepository).save(valido);
    }
}

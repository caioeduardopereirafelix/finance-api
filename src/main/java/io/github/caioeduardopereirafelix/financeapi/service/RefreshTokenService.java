package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidRefreshToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.RefreshToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${api.security.refresh-token.expiration}")
    private long expirationTime;

    @Transactional
    public String generate(User user) {

        String token = SecureTokens.generate();
        Instant now = Instant.now();

        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(SecureTokens.sha256(token))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusMillis(expirationTime))
                .revoked(false)
                .build());

        return token;
    }

    @Transactional
    public User consume(String token) {

        RefreshToken stored = refreshTokenRepository.findByTokenHash(SecureTokens.sha256(token))
                .orElseThrow(() -> new InvalidRefreshToken("Refresh token is invalid"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshToken("Refresh token is expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());

        return stored.getUser();
    }

    @Transactional
    public void revoke(String token) {

        refreshTokenRepository.findByTokenHash(SecureTokens.sha256(token))
                .ifPresent(stored -> {
                    stored.setRevoked(true);
                    refreshTokenRepository.save(stored);
                });
    }

    @Transactional
    public void revokeAllFor(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}

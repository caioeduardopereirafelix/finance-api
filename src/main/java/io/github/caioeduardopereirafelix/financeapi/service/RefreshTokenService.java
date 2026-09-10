package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidRefreshToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.RefreshToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${api.security.refresh-token.expiration}")
    private long expirationTime;

    @Transactional
    public String generate(User user) {

        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant now = Instant.now();

        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(hash(token))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusMillis(expirationTime))
                .revoked(false)
                .build());

        return token;
    }

    @Transactional
    public User consume(String token) {

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(token))
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

        refreshTokenRepository.findByTokenHash(hash(token))
                .ifPresent(stored -> {
                    stored.setRevoked(true);
                    refreshTokenRepository.save(stored);
                });
    }

    @Transactional
    public void revokeAllFor(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

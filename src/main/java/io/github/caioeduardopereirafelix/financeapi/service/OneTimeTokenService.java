package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.model.entity.OneTimeToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.OneTimeTokenRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public abstract class OneTimeTokenService<T extends OneTimeToken> {

    protected abstract OneTimeTokenRepository<T> tokens();

    protected abstract T newToken();

    protected abstract RuntimeException invalidToken();

    protected final String issue(User user, Duration lifetime) {
        Instant now = Instant.now();

        tokens().deleteExpiredBefore(now);
        tokens().deleteByUser(user);

        String raw = SecureTokens.generate();
        T token = newToken();
        token.setTokenHash(SecureTokens.sha256(raw));
        token.setUser(user);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(lifetime));
        tokens().save(token);
        return raw;
    }

    protected final User redeem(String raw) {
        T stored = tokens().findByTokenHash(SecureTokens.sha256(raw))
                .orElseThrow(this::invalidToken);

        if (stored.getExpiresAt().isBefore(Instant.now()) || tokens().consume(stored.getId()) == 0) {
            throw invalidToken();
        }
        return stored.getUser();
    }

    protected final Optional<Duration> cooldownLeft(User user, Duration cooldown) {
        Instant now = Instant.now();
        return tokens().findFirstByUserOrderByCreatedAtDesc(user)
                .map(latest -> latest.getCreatedAt().plus(cooldown))
                .filter(now::isBefore)
                .map(releasedAt -> Duration.between(now, releasedAt));
    }
}

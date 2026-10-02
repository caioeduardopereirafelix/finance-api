package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.TooManyLoginAttemptsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    static final int MAX_TRACKED = 10_000;

    private record Attempts(int failures, Instant firstFailureAt, Instant lockedUntil) {
    }

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(@Value("${api.security.login.max-attempts:5}") int maxAttempts,
                               @Value("${api.security.login.lock-minutes:15}") long lockMinutes) {
        this(maxAttempts, Duration.ofMinutes(lockMinutes), Clock.systemUTC());
    }

    LoginAttemptService(int maxAttempts, Duration lockDuration, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public void checkAllowed(String email) {
        Attempts current = attempts.get(key(email));
        if (current == null || current.lockedUntil() == null) {
            return;
        }
        Instant now = clock.instant();
        if (now.isBefore(current.lockedUntil())) {
            long seconds = Duration.between(now, current.lockedUntil()).toSeconds() + 1;
            throw new TooManyLoginAttemptsException(seconds);
        }
        attempts.remove(key(email));
    }

    public void recordFailure(String email) {
        if (attempts.size() >= MAX_TRACKED) {
            evictStale();
        }
        if (attempts.size() >= MAX_TRACKED && !attempts.containsKey(key(email))) {
            return;
        }
        Instant now = clock.instant();
        attempts.compute(key(email), (k, old) -> {
            int failures = old == null || isStale(old, now) ? 1 : old.failures() + 1;
            Instant firstFailureAt = failures == 1 ? now : old.firstFailureAt();
            Instant lockedUntil = failures >= maxAttempts ? now.plus(lockDuration) : null;
            return new Attempts(failures, firstFailureAt, lockedUntil);
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private boolean isStale(Attempts a, Instant now) {
        if (a.lockedUntil() != null) {
            return !now.isBefore(a.lockedUntil());
        }
        return !now.isBefore(a.firstFailureAt().plus(lockDuration));
    }

    private void evictStale() {
        Instant now = clock.instant();
        attempts.entrySet().removeIf(e -> isStale(e.getValue(), now));
    }

    private static String key(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}

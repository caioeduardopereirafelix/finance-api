package io.github.caioeduardopereirafelix.financeapi.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {

    static final int MAX_TRACKED = 50_000;

    private record Window(Duration length, Deque<Instant> hits) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    public long tryAcquire(String key, int limit, Duration window) {
        Instant now = clock.instant();

        if (windows.size() >= MAX_TRACKED) {
            evictStale(now);
        }
        if (windows.size() >= MAX_TRACKED && !windows.containsKey(key)) {
            return 0;
        }

        Window current = windows.computeIfAbsent(key, k -> new Window(window, new ArrayDeque<>()));
        synchronized (current) {
            Instant cutoff = now.minus(current.length());
            while (!current.hits().isEmpty() && !current.hits().peekFirst().isAfter(cutoff)) {
                current.hits().pollFirst();
            }
            if (current.hits().size() >= limit) {
                long seconds = Duration.between(now, current.hits().peekFirst().plus(current.length())).toSeconds() + 1;
                return Math.max(1, seconds);
            }
            current.hits().addLast(now);
            return 0;
        }
    }

    private void evictStale(Instant now) {
        windows.entrySet().removeIf(entry -> {
            Window w = entry.getValue();
            synchronized (w) {
                return w.hits().isEmpty() || !w.hits().peekLast().plus(w.length()).isAfter(now);
            }
        });
    }
}

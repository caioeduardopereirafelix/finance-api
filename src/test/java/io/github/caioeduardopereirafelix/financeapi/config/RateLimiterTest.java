package io.github.caioeduardopereirafelix.financeapi.config;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-02T12:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final RateLimiter limiter = new RateLimiter(clock);
    private final Duration hour = Duration.ofHours(1);

    @Test
    void permiteAteOLimiteERecusaDepoisComOTempoQueFalta() {
        for (int i = 0; i < 3; i++) {
            assertEquals(0, limiter.tryAcquire("k", 3, hour));
        }

        long wait = limiter.tryAcquire("k", 3, hour);

        assertTrue(wait > 3500 && wait <= 3601, "esperado cerca de uma hora, veio " + wait);
    }

    @Test
    void aJanelaAndaEOsPedidosAntigosSaemDaContagem() {
        limiter.tryAcquire("k", 2, hour);
        clock.now = clock.now.plus(Duration.ofMinutes(30));
        limiter.tryAcquire("k", 2, hour);
        assertTrue(limiter.tryAcquire("k", 2, hour) > 0);

        clock.now = clock.now.plus(Duration.ofMinutes(31));

        assertEquals(0, limiter.tryAcquire("k", 2, hour));
    }

    @Test
    void chavesDiferentesTemContagensIndependentes() {
        assertEquals(0, limiter.tryAcquire("a", 1, hour));
        assertTrue(limiter.tryAcquire("a", 1, hour) > 0);

        assertEquals(0, limiter.tryAcquire("b", 1, hour));
    }

    @Test
    void pedidoRecusadoNaoAumentaAContagem() {
        limiter.tryAcquire("k", 1, hour);
        for (int i = 0; i < 20; i++) {
            limiter.tryAcquire("k", 1, hour);
        }

        clock.now = clock.now.plus(Duration.ofMinutes(61));

        assertEquals(0, limiter.tryAcquire("k", 1, hour));
    }
}

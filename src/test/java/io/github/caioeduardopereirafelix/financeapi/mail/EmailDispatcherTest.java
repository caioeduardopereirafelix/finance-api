package io.github.caioeduardopereirafelix.financeapi.mail;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailDispatcherTest {

    private final SimpleMeterRegistry metrics = new SimpleMeterRegistry();

    private double count(String result) {
        var counter = metrics.find("finance.mail.sent").tag("result", result).counter();
        return counter == null ? 0 : counter.count();
    }

    @Test
    void modoSincronoEntregaNaMesmaThread() {
        List<String> seen = new ArrayList<>();
        var dispatcher = new EmailDispatcher((to, subject, body) -> seen.add(to + "|" + subject + "|" + body),
                false, 3, Duration.ZERO, metrics);

        dispatcher.dispatch("a@test.com", "assunto", "corpo");

        assertEquals(List.of("a@test.com|assunto|corpo"), seen);
        assertEquals(1, count("ok"));
    }

    @Test
    void falhaNoEnvioNaoPropagaParaQuemChamouEViraContagemDeFalha() {
        AtomicInteger calls = new AtomicInteger();
        var dispatcher = new EmailDispatcher((to, subject, body) -> {
            calls.incrementAndGet();
            throw new IllegalStateException("smtp fora do ar");
        }, false, 3, Duration.ZERO, metrics);

        assertDoesNotThrow(() -> dispatcher.dispatch("a@test.com", "assunto", "corpo"));

        assertEquals(3, calls.get());
        assertEquals(1, count("failed"));
        assertEquals(0, count("ok"));
    }

    @Test
    void tentaDeNovoEParaNaPrimeiraQueFunciona() {
        AtomicInteger calls = new AtomicInteger();
        var dispatcher = new EmailDispatcher((to, subject, body) -> {
            if (calls.incrementAndGet() < 3) {
                throw new IllegalStateException("instavel");
            }
        }, false, 5, Duration.ZERO, metrics);

        dispatcher.dispatch("a@test.com", "assunto", "corpo");

        assertEquals(3, calls.get());
        assertEquals(1, count("ok"));
        assertEquals(0, count("failed"));
    }

    @Test
    void modoAssincronoNaoBloqueiaQuemChama() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch delivered = new CountDownLatch(1);
        Thread[] runner = new Thread[1];
        var dispatcher = new EmailDispatcher((to, subject, body) -> {
            runner[0] = Thread.currentThread();
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            delivered.countDown();
        }, true, 3, Duration.ZERO, metrics);

        dispatcher.dispatch("a@test.com", "assunto", "corpo");

        assertEquals(1, delivered.getCount());
        release.countDown();
        assertTrue(delivered.await(5, TimeUnit.SECONDS));
        assertNotEquals(Thread.currentThread(), runner[0]);
    }

    @Test
    void modoAssincronoTentaDeNovoDepoisDaEspera() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch delivered = new CountDownLatch(1);
        var dispatcher = new EmailDispatcher((to, subject, body) -> {
            if (calls.incrementAndGet() < 3) {
                throw new IllegalStateException("instavel");
            }
            delivered.countDown();
        }, true, 3, Duration.ofMillis(1), metrics);

        dispatcher.dispatch("a@test.com", "assunto", "corpo");

        assertTrue(delivered.await(10, TimeUnit.SECONDS));
        assertEquals(3, calls.get());
    }
}

package io.github.caioeduardopereirafelix.financeapi.mail;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailDispatcherTest {

    @Test
    void modoSincronoEntregaNaMesmaThread() {
        List<String> seen = new ArrayList<>();
        var dispatcher = new EmailDispatcher((to, subject, body) -> seen.add(to + "|" + subject + "|" + body), false);

        dispatcher.dispatch("a@test.com", "assunto", "corpo");

        assertEquals(List.of("a@test.com|assunto|corpo"), seen);
    }

    @Test
    void falhaNoEnvioNaoPropagaParaQuemChamou() {
        var dispatcher = new EmailDispatcher((to, subject, body) -> {
            throw new IllegalStateException("smtp fora do ar");
        }, false);

        assertDoesNotThrow(() -> dispatcher.dispatch("a@test.com", "assunto", "corpo"));
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
        }, true);

        dispatcher.dispatch("a@test.com", "assunto", "corpo");

        assertEquals(1, delivered.getCount());
        release.countDown();
        assertTrue(delivered.await(5, TimeUnit.SECONDS));
        assertNotEquals(Thread.currentThread(), runner[0]);
    }
}

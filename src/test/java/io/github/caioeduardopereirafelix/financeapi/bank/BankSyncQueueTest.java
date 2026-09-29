package io.github.caioeduardopereirafelix.financeapi.bank;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BankSyncQueueTest {

    @Test
    void avisosRepetidosEnquantoUmaEstaNaFilaViramUmaSincronizacaoSo() throws Exception {
        BankSyncService syncService = mock(BankSyncService.class);
        CountDownLatch primeiraIniciou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch terminou = new CountDownLatch(2);
        AtomicInteger chamadas = new AtomicInteger();

        when(syncService.syncById(any())).thenAnswer(invocation -> {
            if (chamadas.incrementAndGet() == 1) {
                primeiraIniciou.countDown();
                liberar.await(5, TimeUnit.SECONDS);
            }
            terminou.countDown();
            return new BankSyncService.Result(0, 0);
        });

        BankSyncQueue queue = new BankSyncQueue(syncService);
        UUID id = UUID.randomUUID();

        queue.enqueue(id);
        assertTrue(primeiraIniciou.await(5, TimeUnit.SECONDS));
        // a primeira esta rodando: estes tres avisos pedem UMA nova rodada, nao tres
        queue.enqueue(id);
        queue.enqueue(id);
        queue.enqueue(id);
        liberar.countDown();

        assertTrue(terminou.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);   // tempo para uma terceira execucao indevida aparecer, se existisse
        assertEquals(2, chamadas.get());
        queue.stop();
    }

    @Test
    void falhaDeUmaSincronizacaoNaoMataAFila() throws Exception {
        BankSyncService syncService = mock(BankSyncService.class);
        CountDownLatch segunda = new CountDownLatch(1);
        UUID falha = UUID.randomUUID();
        UUID boa = UUID.randomUUID();
        when(syncService.syncById(falha)).thenThrow(new RuntimeException("boom"));
        when(syncService.syncById(boa)).thenAnswer(invocation -> {
            segunda.countDown();
            return new BankSyncService.Result(0, 0);
        });

        BankSyncQueue queue = new BankSyncQueue(syncService);
        queue.enqueue(falha);
        queue.enqueue(boa);

        assertTrue(segunda.await(5, TimeUnit.SECONDS));
        queue.stop();
    }
}

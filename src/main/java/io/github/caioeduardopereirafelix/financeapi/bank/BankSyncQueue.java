package io.github.caioeduardopereirafelix.financeapi.bank;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fila de sincronizacoes disparadas por webhook.
 *
 * O webhook precisa responder rapido, entao a sincronizacao roda aqui, em segundo plano.
 * Uma unica thread executa uma por vez: dois avisos seguidos da mesma conexao nao correm
 * em paralelo (o que poderia inserir a mesma transacao duas vezes), e avisos repetidos
 * enquanto um ja espera na fila viram uma sincronizacao so.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BankSyncQueue {

    private final BankSyncService syncService;

    private final Set<UUID> waiting = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "bank-sync-queue");
        thread.setDaemon(true);
        return thread;
    });

    public void enqueue(UUID connectionId) {
        if (waiting.add(connectionId)) {
            executor.execute(() -> run(connectionId));
        }
    }

    private void run(UUID connectionId) {
        waiting.remove(connectionId);   // aviso que chegar daqui em diante pede uma nova rodada
        try {
            BankSyncService.Result result = syncService.syncById(connectionId);
            log.info("Conexao {} sincronizada por webhook: {} importadas, {} ignoradas",
                    connectionId, result.imported(), result.skipped());
        } catch (RuntimeException e) {
            log.warn("Falha ao sincronizar a conexao {} por webhook: {}", connectionId, e.getMessage());
        }
    }

    @PreDestroy
    void stop() {
        executor.shutdown();
    }
}

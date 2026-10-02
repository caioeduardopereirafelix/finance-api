package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.repository.BankConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = "bank.sync.enabled", havingValue = "true")
public class BankSyncScheduler {

    private final BankConnectionRepository connections;
    private final BankSyncService syncService;

    @Scheduled(cron = "${bank.sync.cron:0 0 * * * *}")
    public void syncAll() {
        for (BankConnection connection : connections.findAll()) {
            try {
                BankSyncService.Result result = syncService.syncById(connection.getId());
                log.info("Conexao {} sincronizada: {} importadas, {} ignoradas",
                        connection.getId(), result.imported(), result.skipped());
            } catch (RuntimeException e) {
                log.warn("Falha ao sincronizar a conexao {}: {}", connection.getId(), e.getMessage());
            }
        }
    }
}

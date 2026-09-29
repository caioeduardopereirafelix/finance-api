package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.model.enums.BankConnectionStatus;
import io.github.caioeduardopereirafelix.financeapi.repository.BankConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sincroniza todas as conexoes ativas em intervalo fixo.
 *
 * Desligado por padrao. Ligue com BANK_SYNC_ENABLED=true; o horario vem de
 * bank.sync.cron (padrao: de hora em hora).
 */
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
        for (BankConnection connection : connections.findByStatus(BankConnectionStatus.ACTIVE)) {
            try {
                BankSyncService.Result result = syncService.syncById(connection.getId());
                log.info("Conexao {} sincronizada: {} importadas, {} ignoradas",
                        connection.getId(), result.imported(), result.skipped());
            } catch (RuntimeException e) {
                // Uma conexao com problema nao pode impedir as outras.
                log.warn("Falha ao sincronizar a conexao {}: {}", connection.getId(), e.getMessage());
            }
        }
    }
}

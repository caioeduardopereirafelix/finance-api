package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.service.CategoryRuleService;
import io.github.caioeduardopereirafelix.financeapi.service.DescriptionKey;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.BankConnectionStatus;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionSource;
import io.github.caioeduardopereirafelix.financeapi.repository.BankConnectionRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankSyncService {

    static final Duration INITIAL_WINDOW = Duration.ofDays(90);

    static final Duration OVERLAP = Duration.ofDays(7);

    public record Result(int imported, int skipped) {
    }

    private final BankConnectionRepository connections;
    private final TransactionRepository transactions;
    private final BankProviders providers;
    private final BankCategoryMapper categoryMapper;
    private final CategoryRuleService categoryRules;

    @Transactional(noRollbackFor = BankIntegrationException.class)
    public Result syncForUser(UUID connectionId, User user) {
        BankConnection connection = connections.findByIdAndUser(connectionId, user)
                .orElseThrow(() -> new BankIntegrationException(HttpStatus.NOT_FOUND, "Conexao bancaria nao encontrada"));
        return sync(connection);
    }

    @Transactional(noRollbackFor = BankIntegrationException.class)
    public Result syncById(UUID connectionId) {
        BankConnection connection = connections.findById(connectionId)
                .orElseThrow(() -> new BankIntegrationException(HttpStatus.NOT_FOUND, "Conexao bancaria nao encontrada"));
        return sync(connection);
    }

    private void markError(BankConnection connection, RuntimeException cause) {
        log.warn("Falha ao sincronizar a conexao {} ({}): {}", connection.getId(), connection.getProvider(), cause.toString());
        connection.setStatus(BankConnectionStatus.ERROR);
        connections.save(connection);
    }

    private Result sync(BankConnection connection) {
        BankProvider provider = providers.named(connection.getProvider());
        Instant startedAt = Instant.now();
        Instant since = connection.getLastSyncedAt() == null
                ? startedAt.minus(INITIAL_WINDOW)
                : connection.getLastSyncedAt().minus(OVERLAP);

        List<ExternalTransaction> fetched;
        try {
            fetched = provider.fetchTransactions(connection.getExternalId(), since);
        } catch (BankIntegrationException e) {
            markError(connection, e);
            throw e;
        } catch (RuntimeException e) {
            markError(connection, e);
            throw new BankIntegrationException(HttpStatus.BAD_GATEWAY,
                    "Nao foi possivel buscar as movimentacoes no provedor bancario", e);
        }

        int imported = 0;
        int skipped = 0;
        User user = connection.getUser();
        Map<String, CategoryName> userRules = categoryRules.rulesOf(user);

        for (ExternalTransaction external : fetched) {
            String externalId = connection.getProvider() + ":" + external.id();

            boolean unusable = external.amount() == null || external.amount().signum() == 0;
            if (unusable || transactions.existsByUserAndExternalId(user, externalId)) {
                skipped++;
                continue;
            }

            BankCategoryMapper.Mapped mapped = categoryMapper.map(external.amount(), external.category());
            CategoryName category = userRules.getOrDefault(
                    CategoryRuleService.lookupKey(mapped.type(), DescriptionKey.of(external.description())),
                    mapped.category());

            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setCreatedBy(user.getId().toString());
            transaction.setDescription(external.description());
            transaction.setAmount(external.amount().abs());
            transaction.setType(mapped.type());
            transaction.setCategory(category);
            transaction.setSource(TransactionSource.BANK);
            transaction.setExternalId(externalId);
            transaction.setOccurredAt(external.date());
            transaction.setBankConnection(connection);
            transactions.save(transaction);
            imported++;
        }

        connection.setStatus(BankConnectionStatus.ACTIVE);
        connection.setLastSyncedAt(startedAt);
        connections.save(connection);

        return new Result(imported, skipped);
    }
}

package io.github.caioeduardopereirafelix.financeapi.bank.pluggy;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.caioeduardopereirafelix.financeapi.bank.BankSyncQueue;
import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.repository.BankConnectionRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("!'${bank.pluggy.webhook-secret:}'.isBlank()")
public class PluggyWebhookService {

    private static final Pattern UUID_FORMAT = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final int DELETE_CHUNK = 500;
    private static final int MAX_IDS = 5_000;

    private final BankConnectionRepository connections;
    private final TransactionRepository transactions;
    private final BankSyncQueue syncQueue;

    @Transactional
    public void handle(JsonNode payload) {
        String event = payload.path("event").asText("");
        String itemId = payload.path("itemId").asText("");

        if (!event.startsWith("transactions/")) {
            return;
        }
        if (!UUID_FORMAT.matcher(itemId).matches()) {
            log.warn("Webhook {} ignorado: itemId invalido", event);
            return;
        }
        BankConnection connection = connections.findByProviderAndExternalId(PluggyBankProvider.NAME, itemId)
                .orElse(null);
        if (connection == null) {
            log.info("Webhook {} ignorado: item {} nao esta conectado aqui", event, itemId);
            return;
        }

        switch (event) {
            case "transactions/created", "transactions/updated" -> syncQueue.enqueue(connection.getId());
            case "transactions/deleted" -> deleteTransactions(connection, payload.path("transactionIds"));
            default -> log.debug("Webhook {} nao tratado", event);
        }
    }

    private void deleteTransactions(BankConnection connection, JsonNode ids) {
        if (!ids.isArray() || ids.isEmpty()) {
            return;
        }
        List<String> externalIds = new ArrayList<>();
        for (JsonNode id : ids) {
            if (id.isTextual() && !id.asText().isBlank() && externalIds.size() < MAX_IDS) {
                externalIds.add(PluggyBankProvider.NAME + ":" + id.asText());
            }
        }
        int deleted = 0;
        for (int from = 0; from < externalIds.size(); from += DELETE_CHUNK) {
            deleted += transactions.deleteImported(connection,
                    externalIds.subList(from, Math.min(from + DELETE_CHUNK, externalIds.size())));
        }
        log.info("Webhook: {} transacoes apagadas na conexao {}", deleted, connection.getId());
    }
}

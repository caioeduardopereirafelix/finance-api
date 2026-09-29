package io.github.caioeduardopereirafelix.financeapi.bank;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Provedor de mentira, para desenvolvimento, demonstracao e testes.
 *
 * Desligado por padrao: em producao ele deixaria qualquer usuario "conectar"
 * um banco falso. Para usar, defina BANK_MOCK_ENABLED=true.
 *
 * Os ids sao fixos, entao sincronizar duas vezes nao duplica nada.
 */
@Component
@ConditionalOnProperty(name = "bank.mock.enabled", havingValue = "true")
public class MockBankProvider implements BankProvider {

    public static final String NAME = "mock";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String createConnectToken(String userReference) {
        return "mock-connect-token";
    }

    @Override
    public ExternalConnection describeConnection(String externalId, String userReference) {
        return new ExternalConnection(externalId, "Banco Demo");
    }

    @Override
    public void disconnect(String externalId) {
        // nada a revogar: o provedor de mentira nao guarda autorizacao nenhuma
    }

    @Override
    public List<ExternalTransaction> fetchTransactions(String externalId, Instant since) {
        Instant now = Instant.now();

        List<ExternalTransaction> all = List.of(
                new ExternalTransaction(externalId + "-1", "Supermercado Central", new BigDecimal("-312.48"), now.minus(2, ChronoUnit.DAYS), "Groceries"),
                new ExternalTransaction(externalId + "-2", "Uber *Viagem", new BigDecimal("-27.90"), now.minus(3, ChronoUnit.DAYS), "Transport"),
                new ExternalTransaction(externalId + "-3", "Farmacia Saude", new BigDecimal("-84.10"), now.minus(5, ChronoUnit.DAYS), "Pharmacy"),
                new ExternalTransaction(externalId + "-4", "Streaming Mensal", new BigDecimal("-39.90"), now.minus(8, ChronoUnit.DAYS), "Entertainment"),
                new ExternalTransaction(externalId + "-5", "Salario Empresa X", new BigDecimal("5400.00"), now.minus(10, ChronoUnit.DAYS), "Salary"),
                new ExternalTransaction(externalId + "-6", "Pix recebido", new BigDecimal("150.00"), now.minus(12, ChronoUnit.DAYS), null),
                new ExternalTransaction(externalId + "-7", "Tarifa desconhecida", new BigDecimal("-9.90"), now.minus(20, ChronoUnit.DAYS), "Misc Fees")
        );

        return all.stream().filter(t -> !t.date().isBefore(since)).toList();
    }
}

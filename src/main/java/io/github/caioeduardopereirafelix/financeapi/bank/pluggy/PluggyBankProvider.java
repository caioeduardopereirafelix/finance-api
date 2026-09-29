package io.github.caioeduardopereirafelix.financeapi.bank.pluggy;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.caioeduardopereirafelix.financeapi.bank.BankIntegrationException;
import io.github.caioeduardopereirafelix.financeapi.bank.BankProvider;
import io.github.caioeduardopereirafelix.financeapi.bank.ExternalConnection;
import io.github.caioeduardopereirafelix.financeapi.bank.ExternalTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Agregador Pluggy. O "externalId" de uma conexao nossa e o id do item da Pluggy
 * (um banco autorizado por um usuario).
 *
 * Decisoes de importacao:
 * - Movimentacao PENDING e ignorada: ela ainda nao afetou o saldo (compras da
 *   fatura aberta e parcelas futuras do cartao ficam PENDING) e pode mudar.
 *   Entra quando virar POSTED; a Pluggy mantem o mesmo id nessa passagem.
 * - Cartao de credito fica de fora por padrao. O pagamento da fatura ja sai da
 *   conta corrente, entao importar as compras do cartao tambem contaria o gasto
 *   duas vezes.
 * - Com cartao ligado, entram as compras (DEBIT) e os estornos/cashback (viram
 *   entrada). Pagamento de fatura e qualquer outro credito do cartao ficam de fora.
 * - Valor em moeda estrangeira: usa amountInAccountCurrency; sem ele, ignora,
 *   porque gravar 10 USD como R$ 10 seria um valor errado.
 */
@Slf4j
public class PluggyBankProvider implements BankProvider {

    public static final String NAME = "pluggy";

    /** Item ids da Pluggy sao UUIDs. Validar evita que texto livre do cliente vire caminho de URL. */
    private static final Pattern UUID_FORMAT = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static final int DESCRIPTION_MAX = 255;
    private static final int INSTITUTION_MAX = 120;

    /**
     * Compras da fatura aberta ficam PENDING e so viram POSTED quando a fatura
     * fecha, com a data original da compra (ate ~30 dias antes). Para o cartao a
     * busca volta sempre esse tanto; os ja importados sao descartados pelo external_id.
     */
    static final Duration CARD_LOOKBACK = Duration.ofDays(60);

    private static final String BRL = "BRL";

    private final PluggyClient client;
    private final boolean includeCreditCards;
    private final ZoneId zone;
    private final Clock clock;

    public PluggyBankProvider(PluggyClient client, boolean includeCreditCards, ZoneId zone, Clock clock) {
        this.client = client;
        this.includeCreditCards = includeCreditCards;
        this.zone = zone;
        this.clock = clock;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String createConnectToken(String userReference) {
        try {
            return client.createConnectToken(userReference);
        } catch (BankIntegrationException e) {
            throw e;   // ja traz o que a Pluggy respondeu
        } catch (RuntimeException e) {
            throw new BankIntegrationException(HttpStatus.BAD_GATEWAY,
                    "Nao foi possivel iniciar a conexao com a Pluggy", e);
        }
    }

    @Override
    public ExternalConnection describeConnection(String externalId, String userReference) {
        String itemId = requireItemId(externalId);
        JsonNode item = client.item(itemId);
        requireOwnedBy(item, userReference);
        String institution = item.path("connector").path("name").asText(null);
        return new ExternalConnection(itemId, truncate(institution, INSTITUTION_MAX));
    }

    @Override
    public void disconnect(String externalId) {
        client.deleteItem(requireItemId(externalId));
    }

    @Override
    public List<ExternalTransaction> fetchTransactions(String externalId, Instant since) {
        String itemId = requireItemId(externalId);
        Instant cardSince = since.isBefore(clock.instant().minus(CARD_LOOKBACK))
                ? since
                : clock.instant().minus(CARD_LOOKBACK);

        List<ExternalTransaction> result = new ArrayList<>();
        for (JsonNode account : client.accounts(itemId)) {
            boolean creditCard = isCreditCard(account);
            if (isCredit(account) && !creditCard) {
                continue;   // emprestimo: nao e gasto do dia a dia
            }
            if (creditCard && !includeCreditCards) {
                continue;
            }
            LocalDate from = (creditCard ? cardSince : since).atZone(ZoneOffset.UTC).toLocalDate();
            for (JsonNode raw : client.transactions(account.path("id").asText(), from)) {
                ExternalTransaction mapped = map(raw, creditCard);
                if (mapped != null) {
                    result.add(mapped);
                }
            }
        }
        return result;
    }

    /**
     * O connect token foi criado com clientUserId = id do nosso usuario, e a Pluggy
     * grava isso no item. Um item de outro usuario e recusado.
     *
     * Se a Pluggy nao devolver o clientUserId, nao da para conferir: aceita e avisa
     * no log. [confirmar no sandbox que o campo vem preenchido]
     */
    private static void requireOwnedBy(JsonNode item, String userReference) {
        String owner = item.path("clientUserId").asText(null);
        if (owner == null || owner.isBlank()) {
            log.warn("Item {} veio sem clientUserId; nao foi possivel conferir o dono", item.path("id").asText());
            return;
        }
        if (!owner.equals(userReference)) {
            throw new BankIntegrationException(HttpStatus.FORBIDDEN, "Esta conexao bancaria pertence a outro usuario");
        }
    }

    // ---------- traducao ----------

    private static boolean isCredit(JsonNode account) {
        return "CREDIT".equalsIgnoreCase(account.path("type").asText());
    }

    /** Conta CREDIT tambem cobre emprestimos; so o cartao (ou sem subtipo informado) interessa. */
    private static boolean isCreditCard(JsonNode account) {
        String subtype = account.path("subtype").asText("");
        return isCredit(account) && (subtype.isBlank() || "CREDIT_CARD".equalsIgnoreCase(subtype));
    }

    /** @return a movimentacao no nosso formato, ou null se ela nao deve ser importada */
    ExternalTransaction map(JsonNode raw, boolean creditCardAccount) {
        String id = raw.path("id").asText(null);
        JsonNode amountNode = raw.path("amount");
        if (id == null || id.isBlank() || !amountNode.isNumber()) {
            return null;
        }
        if ("PENDING".equalsIgnoreCase(raw.path("status").asText())) {
            return null;
        }

        BigDecimal amount = amountInAccountCurrency(raw, amountNode.decimalValue());
        if (amount == null) {
            return null;
        }

        BigDecimal signed = signedAmount(raw.path("type").asText(""), amount, creditCardAccount,
                raw.path("operationType").asText(""));
        Instant date = parseDate(raw.path("date").asText(null));
        if (signed == null || date == null) {
            return null;
        }

        String description = raw.path("description").asText("");
        if (description.isBlank()) {
            description = raw.path("descriptionRaw").asText("");
        }
        String category = raw.path("category").asText(null);

        return new ExternalTransaction(id, truncate(description, DESCRIPTION_MAX), signed, date, category);
    }

    /**
     * "amount" vem na moeda da transacao. Se nao for real, o valor em reais e o
     * amountInAccountCurrency; sem ele nao ha como converter.
     */
    private static BigDecimal amountInAccountCurrency(JsonNode raw, BigDecimal amount) {
        String currency = raw.path("currencyCode").asText(BRL);
        if (currency.isBlank() || BRL.equalsIgnoreCase(currency)) {
            return amount;
        }
        JsonNode converted = raw.path("amountInAccountCurrency");
        return converted.isNumber() ? converted.decimalValue() : null;
    }

    /**
     * O sinal sai do campo "type" (DEBIT = saiu, CREDIT = entrou), nao do sinal do
     * "amount": no cartao a convencao e outra (positivo = compra, negativo =
     * pagamento). No cartao, um credito so entra se for estorno ou cashback;
     * pagamento de fatura e o resto ficam de fora.
     */
    private static BigDecimal signedAmount(String type, BigDecimal amount, boolean creditCardAccount,
                                           String operationType) {
        BigDecimal abs = amount.abs();
        if ("DEBIT".equalsIgnoreCase(type)) {
            return abs.negate();
        }
        if ("CREDIT".equalsIgnoreCase(type)) {
            boolean refund = "ESTORNO".equalsIgnoreCase(operationType) || "CASHBACK".equalsIgnoreCase(operationType);
            return creditCardAccount && !refund ? null : abs;
        }
        return creditCardAccount ? null : amount;
    }

    /**
     * "date" e ISO 8601 em UTC. Muitas movimentacoes chegam so com o dia, como
     * meia-noite UTC (o proprio exemplo da documentacao), que no fuso do Brasil
     * seria 21h do dia anterior: o lancamento apareceria no dia errado. Meia-noite
     * exata vira o comeco daquele dia no fuso da aplicacao; qualquer outro
     * horario segue como veio. [conferir com dados reais no sandbox]
     */
    private Instant parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            Instant instant = OffsetDateTime.parse(text).toInstant();
            if (instant.atZone(ZoneOffset.UTC).toLocalTime().equals(LocalTime.MIDNIGHT)) {
                return instant.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant();
            }
            return instant;
        } catch (DateTimeParseException notDateTime) {
            try {
                return LocalDate.parse(text.substring(0, Math.min(10, text.length()))).atStartOfDay(zone).toInstant();
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }

    private static String requireItemId(String externalId) {
        if (externalId == null || !UUID_FORMAT.matcher(externalId).matches()) {
            throw new BankIntegrationException(HttpStatus.BAD_REQUEST, "Identificador de conexao invalido");
        }
        return externalId;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}

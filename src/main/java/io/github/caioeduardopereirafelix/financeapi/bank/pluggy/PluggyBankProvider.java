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

@Slf4j
public class PluggyBankProvider implements BankProvider {

    public static final String NAME = "pluggy";

    private static final Pattern UUID_FORMAT = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private static final int DESCRIPTION_MAX = 255;
    private static final int INSTITUTION_MAX = 120;

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
            throw e;
        } catch (RuntimeException e) {
            throw new BankIntegrationException(HttpStatus.BAD_GATEWAY,
                    "Nao foi possivel iniciar a conexao com a Pluggy", e);
        }
    }

    @Override
    public String createUpdateToken(String externalId, String userReference) {
        String itemId = requireItemId(externalId);
        try {
            return client.createConnectToken(userReference, itemId);
        } catch (BankIntegrationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BankIntegrationException(HttpStatus.BAD_GATEWAY,
                    "Nao foi possivel iniciar a reautorizacao na Pluggy", e);
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
                continue;
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

    private static boolean isCredit(JsonNode account) {
        return "CREDIT".equalsIgnoreCase(account.path("type").asText());
    }

    private static boolean isCreditCard(JsonNode account) {
        String subtype = account.path("subtype").asText("");
        return isCredit(account) && (subtype.isBlank() || "CREDIT_CARD".equalsIgnoreCase(subtype));
    }

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

    private static BigDecimal amountInAccountCurrency(JsonNode raw, BigDecimal amount) {
        String currency = raw.path("currencyCode").asText(BRL);
        if (currency.isBlank() || BRL.equalsIgnoreCase(currency)) {
            return amount;
        }
        JsonNode converted = raw.path("amountInAccountCurrency");
        return converted.isNumber() ? converted.decimalValue() : null;
    }

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

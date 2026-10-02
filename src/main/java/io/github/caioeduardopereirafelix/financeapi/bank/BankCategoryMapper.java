package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

@Component
public class BankCategoryMapper {

    public record Mapped(TransactionalType type, CategoryName category) {
    }

    private static final Map<String, CategoryName> KNOWN = Map.ofEntries(
            Map.entry("groceries", CategoryName.FOOD),
            Map.entry("supermarkets", CategoryName.FOOD),
            Map.entry("restaurants", CategoryName.FOOD),
            Map.entry("eating out", CategoryName.FOOD),
            Map.entry("food", CategoryName.FOOD),
            Map.entry("mercado", CategoryName.FOOD),
            Map.entry("alimentacao", CategoryName.FOOD),
            Map.entry("restaurantes", CategoryName.FOOD),

            Map.entry("transport", CategoryName.TRANSPORT),
            Map.entry("transportation", CategoryName.TRANSPORT),
            Map.entry("taxi", CategoryName.TRANSPORT),
            Map.entry("ride hailing", CategoryName.TRANSPORT),
            Map.entry("fuel", CategoryName.TRANSPORT),
            Map.entry("gas stations", CategoryName.TRANSPORT),
            Map.entry("taxi and ride-hailing", CategoryName.TRANSPORT),
            Map.entry("public transportation", CategoryName.TRANSPORT),
            Map.entry("transporte", CategoryName.TRANSPORT),
            Map.entry("combustivel", CategoryName.TRANSPORT),

            Map.entry("health", CategoryName.HEALTH),
            Map.entry("pharmacy", CategoryName.HEALTH),
            Map.entry("saude", CategoryName.HEALTH),
            Map.entry("farmacia", CategoryName.HEALTH),

            Map.entry("rent", CategoryName.HOUSING),
            Map.entry("housing", CategoryName.HOUSING),
            Map.entry("moradia", CategoryName.HOUSING),
            Map.entry("aluguel", CategoryName.HOUSING),

            Map.entry("entertainment", CategoryName.LEISURE),
            Map.entry("leisure", CategoryName.LEISURE),
            Map.entry("streaming", CategoryName.LEISURE),
            Map.entry("lazer", CategoryName.LEISURE),

            Map.entry("utilities", CategoryName.BILLS),
            Map.entry("bills", CategoryName.BILLS),
            Map.entry("contas", CategoryName.BILLS),

            Map.entry("investments", CategoryName.INVESTMENTS),
            Map.entry("fixed income investment", CategoryName.INVESTMENTS),
            Map.entry("investimentos", CategoryName.INVESTMENTS),

            Map.entry("salary", CategoryName.WAGE),
            Map.entry("payroll", CategoryName.WAGE),
            Map.entry("salario", CategoryName.WAGE),

            Map.entry("extra income", CategoryName.EXTRA_INCOME),
            Map.entry("renda extra", CategoryName.EXTRA_INCOME)
    );

    public Mapped map(BigDecimal signedAmount, String providerCategory) {
        TransactionalType type = signedAmount.signum() < 0
                ? TransactionalType.EXPENSES
                : TransactionalType.CASH_ENTRY;

        CategoryName found = KNOWN.get(normalize(providerCategory));

        if (found != null && found.getTransactionalType() == type) {
            return new Mapped(type, found);
        }
        return new Mapped(type, type == TransactionalType.EXPENSES
                ? CategoryName.OTHER_EXPENSE
                : CategoryName.OTHER_INCOME);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.trim().toLowerCase(Locale.ROOT);
    }
}

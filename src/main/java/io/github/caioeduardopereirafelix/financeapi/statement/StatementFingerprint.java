package io.github.caioeduardopereirafelix.financeapi.statement;

import io.github.caioeduardopereirafelix.financeapi.service.SecureTokens;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class StatementFingerprint {

    private final Map<String, Integer> seen = new HashMap<>();

    String of(Instant date, BigDecimal amount, String description) {
        String base = date.toString() + "|" + amount.toPlainString() + "|"
                + description.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        int occurrence = seen.merge(base, 1, Integer::sum);
        return SecureTokens.sha256(base + "|" + occurrence).substring(0, 40);
    }
}

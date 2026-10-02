package io.github.caioeduardopereirafelix.financeapi.bank;

import java.math.BigDecimal;
import java.time.Instant;

public record ExternalTransaction(
        String id,
        String description,
        BigDecimal amount,
        Instant date,
        String category) {
}

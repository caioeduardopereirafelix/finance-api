package io.github.caioeduardopereirafelix.financeapi.statement;

import java.math.BigDecimal;
import java.time.Instant;

public record StatementEntry(String id, String description, BigDecimal amount, Instant date, String category) {
}

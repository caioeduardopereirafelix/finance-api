package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionFilter(
        TransactionalType type,
        CategoryName category,
        String description,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Instant occurredFrom,
        Instant occurredBefore) {
}

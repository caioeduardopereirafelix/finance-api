package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;

import java.math.BigDecimal;

public interface TransactionSummaryProjection {

    TransactionalType getType();

    BigDecimal getTotal();
}

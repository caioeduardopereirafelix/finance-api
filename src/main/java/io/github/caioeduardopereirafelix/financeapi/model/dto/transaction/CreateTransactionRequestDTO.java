package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequestDTO(
        String description,
        BigDecimal amount,
        TransactionalType type,
        CategoryName category,
        UUID id) {
}

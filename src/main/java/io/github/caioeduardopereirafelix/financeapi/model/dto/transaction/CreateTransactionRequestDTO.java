package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequestDTO(
        @NotNull
        String description,
        @NotNull
        BigDecimal amount,
        @NotNull
        TransactionalType type,
        @NotNull
        CategoryName category,
        @NotNull
        UUID userId) {
}

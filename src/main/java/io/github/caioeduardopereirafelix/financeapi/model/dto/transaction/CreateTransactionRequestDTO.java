package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequestDTO(
        @NotBlank(message = "Description cannot be empty")
        String description,
        @NotNull(message = "Amount cannot be empty")
        @Positive(message = "Amount cannot be less than 0")
        BigDecimal amount,
        @NotNull(message = "Transactional Type cannot be empty")
        TransactionalType type,
        @NotNull(message = "Category Name cannot be empty")
        CategoryName category) {
}

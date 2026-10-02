package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import jakarta.validation.constraints.NotNull;

public record UpdateCategoryRequestDTO(
        @NotNull(message = "Category Name cannot be empty")
        CategoryName category,
        boolean applyToSimilar
) {
}

package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import jakarta.validation.constraints.NotNull;

/**
 * @param applyToSimilar tambem usa a categoria nas transacoes importadas do mesmo estabelecimento
 *                       e guarda a escolha para as proximas importacoes
 */
public record UpdateCategoryRequestDTO(
        @NotNull(message = "Category Name cannot be empty")
        CategoryName category,
        boolean applyToSimilar
) {
}

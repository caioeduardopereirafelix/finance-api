package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;

import java.math.BigDecimal;

/** Total de uma categoria no periodo, com quantas transacoes o compoem. */
public record CategoryTotalDTO(
        CategoryName category,
        TransactionalType type,
        BigDecimal total,
        long count
) {
}

package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;

import java.math.BigDecimal;

public record CategoryTotalDTO(
        CategoryName category,
        TransactionalType type,
        BigDecimal total,
        long count
) {
}

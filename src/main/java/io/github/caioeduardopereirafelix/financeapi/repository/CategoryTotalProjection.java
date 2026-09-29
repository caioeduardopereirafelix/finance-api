package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;

import java.math.BigDecimal;

public interface CategoryTotalProjection {

    CategoryName getCategory();

    TransactionalType getType();

    BigDecimal getTotal();

    Long getCount();
}

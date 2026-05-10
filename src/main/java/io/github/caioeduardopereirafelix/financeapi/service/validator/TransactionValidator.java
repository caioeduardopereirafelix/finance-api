package io.github.caioeduardopereirafelix.financeapi.service.validator;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionValidator {

    public void validateCategoryByType(CategoryName category, TransactionalType type){
        if (category.getTransactionalType() != type){
            throw new IllegalArgumentException("" +
                    "Categoria invalida para esse tipo de transacao");
        }
    }

    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da transação deve ser maior que zero");
        }
    }
}

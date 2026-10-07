package io.github.caioeduardopereirafelix.financeapi.service.validator;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.UpdateTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import org.springframework.stereotype.Component;


@Component
public class TransactionValidator {

    public void validateCategoryByType(CategoryName category, TransactionalType type){
        if (category.getTransactionalType() != type){
            throw new InvalidFieldException("category",
                    "Category does not match the transaction type");
        }
    }

    public void validateImportedOnlyChangesCategory(Transaction transaction, UpdateTransactionDTO changes) {
        if (!transaction.getSource().imported()) {
            return;
        }
        boolean changed = transaction.getType() != changes.type()
                || transaction.getAmount().compareTo(changes.amount()) != 0
                || !java.util.Objects.equals(transaction.getDescription(), changes.description());
        if (changed) {
            throw new InvalidFieldException("transaction",
                    "Transacao importada: so a categoria pode ser alterada");
        }
    }
}

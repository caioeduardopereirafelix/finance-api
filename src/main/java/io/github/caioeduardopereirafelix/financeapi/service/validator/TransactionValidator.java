package io.github.caioeduardopereirafelix.financeapi.service.validator;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionSource;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.dto.transaction.UpdateTransactionDTO;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TransactionValidator {

    public void validateCategoryByType(CategoryName category, TransactionalType type){
        if (category.getTransactionalType() != type){
            throw new InvalidFieldException("category",
                    "Category does not match the transaction type");
        }
    }

    /**
     * Uma transacao importada reflete o que o banco informou: valor, descricao e tipo nao se
     * mudam a mao. A categoria, sim.
     */
    public void validateImportedOnlyChangesCategory(Transaction transaction, UpdateTransactionDTO changes) {
        if (transaction.getSource() != TransactionSource.BANK) {
            return;
        }
        boolean changed = transaction.getType() != changes.type()
                || transaction.getAmount().compareTo(changes.amount()) != 0
                || !java.util.Objects.equals(transaction.getDescription(), changes.description());
        if (changed) {
            throw new InvalidFieldException("transaction",
                    "Transacao importada do banco: so a categoria pode ser alterada");
        }
    }

    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidFieldException("amount", "Amount must be greater than zero");
        }
    }
}

package io.github.caioeduardopereirafelix.financeapi.service.validator;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionValidatorTest {

    private TransactionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransactionValidator();
    }

    @Test
    void deveLancarExcecaoQuandoCategoriaNaoCombinaComOTipo() {
        assertThrows(
                InvalidFieldException.class,
                () -> validator.validateCategoryByType(CategoryName.FOOD, TransactionalType.CASH_ENTRY)
        );
    }

    @Test
    void deveAceitarCategoriaCoerenteComOTipo() {
        assertDoesNotThrow(
                () -> validator.validateCategoryByType(CategoryName.WAGE, TransactionalType.CASH_ENTRY)
        );
        assertDoesNotThrow(
                () -> validator.validateCategoryByType(CategoryName.FOOD, TransactionalType.EXPENSES)
        );
    }

    @Test
    void categoriasOutrosRespeitamOTipo() {
        assertDoesNotThrow(() -> validator.validateCategoryByType(CategoryName.OTHER_EXPENSE, TransactionalType.EXPENSES));
        assertDoesNotThrow(() -> validator.validateCategoryByType(CategoryName.OTHER_INCOME, TransactionalType.CASH_ENTRY));
        assertThrows(InvalidFieldException.class,
                () -> validator.validateCategoryByType(CategoryName.OTHER_EXPENSE, TransactionalType.CASH_ENTRY));
        assertThrows(InvalidFieldException.class,
                () -> validator.validateCategoryByType(CategoryName.OTHER_INCOME, TransactionalType.EXPENSES));
    }
}

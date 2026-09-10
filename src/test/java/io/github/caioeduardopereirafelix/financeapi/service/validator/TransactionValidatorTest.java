package io.github.caioeduardopereirafelix.financeapi.service.validator;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionValidatorTest {

    private TransactionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransactionValidator();
    }

    @Test
    void deveLancarExcecaoQuandoValorForNegativo() {
        BigDecimal amount = new BigDecimal("-100");

        assertThrows(
                InvalidFieldException.class,
                () -> validator.validateAmount(amount)
        );
    }

    @Test
    void deveLancarExcecaoQuandoValorForZero() {
        assertThrows(
                InvalidFieldException.class,
                () -> validator.validateAmount(BigDecimal.ZERO)
        );
    }

    @Test
    void deveLancarExcecaoQuandoValorForNulo() {
        assertThrows(
                InvalidFieldException.class,
                () -> validator.validateAmount(null)
        );
    }

    @Test
    void deveAceitarValorPositivo() {
        assertDoesNotThrow(() -> validator.validateAmount(new BigDecimal("10.50")));
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
}

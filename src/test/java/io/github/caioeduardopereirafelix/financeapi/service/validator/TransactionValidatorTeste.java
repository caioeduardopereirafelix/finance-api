package io.github.caioeduardopereirafelix.financeapi.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionValidatorTeste {

    private TransactionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransactionValidator();
    }

    @Test
    void deveLancarExcecaoQuandoValorForNegativo() {
        BigDecimal amount = new BigDecimal("-100");

        assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateAmount(amount)
        );
    }
}


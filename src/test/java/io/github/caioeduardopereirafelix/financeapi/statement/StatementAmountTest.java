package io.github.caioeduardopereirafelix.financeapi.statement;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatementAmountTest {

    private static BigDecimal v(String text) {
        return new BigDecimal(text);
    }

    @Test
    void entendeOFormatoBrasileiroComMilharEVirgula() {
        assertEquals(v("1234.56"), StatementAmount.parse("1.234,56"));
        assertEquals(v("-1234.56"), StatementAmount.parse("-1.234,56"));
        assertEquals(v("1.50"), StatementAmount.parse("1,5"));
        assertEquals(v("1000000.00"), StatementAmount.parse("1.000.000,00"));
    }

    @Test
    void entendeOFormatoComPontoDecimal() {
        assertEquals(v("-42.50"), StatementAmount.parse("-42.50"));
        assertEquals(v("1234.56"), StatementAmount.parse("1,234.56"));
        assertEquals(v("12.50"), StatementAmount.parse("12.5"));
    }

    @Test
    void pontoSeguidoDeTresDigitosSemDecimalEMilhar() {
        assertEquals(v("1234.00"), StatementAmount.parse("1.234"));
        assertEquals(v("1234567.00"), StatementAmount.parse("1.234.567"));
    }

    @Test
    void aceitaMoedaParentesesSinalNoFimEMenosTipografico() {
        assertEquals(v("1000.00"), StatementAmount.parse("R$ 1.000,00"));
        assertEquals(v("-10.00"), StatementAmount.parse("(10,00)"));
        assertEquals(v("-10.00"), StatementAmount.parse("10,00-"));
        assertEquals(v("-5.00"), StatementAmount.parse("−5,00"));
        assertEquals(v("7.00"), StatementAmount.parse("+7,00"));
    }

    @Test
    void vazioDevolveNuloELixoLancaExcecao() {
        assertNull(StatementAmount.parse(null));
        assertNull(StatementAmount.parse("   "));
        assertThrows(NumberFormatException.class, () -> StatementAmount.parse("abc"));
        assertThrows(NumberFormatException.class, () -> StatementAmount.parse("12a,00"));
    }

    @Test
    void arredondaParaDuasCasas() {
        assertEquals(v("10.13"), StatementAmount.parse("10,125"));
    }
}

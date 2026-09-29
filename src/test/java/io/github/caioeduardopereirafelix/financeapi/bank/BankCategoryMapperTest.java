package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BankCategoryMapperTest {

    private final BankCategoryMapper mapper = new BankCategoryMapper();

    @Test
    void categoriaEmInglesDaDocumentacaoDaPluggyViraInvestimento() {
        var mapped = mapper.map(new java.math.BigDecimal("-212.45"), "Fixed Income Investment");

        org.junit.jupiter.api.Assertions.assertEquals(
                io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName.INVESTMENTS, mapped.category());
    }

    @Test
    void valorNegativoEhSaidaEUsaACategoriaConhecida() {
        var mapped = mapper.map(new BigDecimal("-50"), "Groceries");

        assertEquals(TransactionalType.EXPENSES, mapped.type());
        assertEquals(CategoryName.FOOD, mapped.category());
    }

    @Test
    void valorPositivoEhEntrada() {
        var mapped = mapper.map(new BigDecimal("5400"), "Salary");

        assertEquals(TransactionalType.CASH_ENTRY, mapped.type());
        assertEquals(CategoryName.WAGE, mapped.category());
    }

    @Test
    void ignoraAcentosEMaiusculas() {
        assertEquals(CategoryName.HEALTH, mapper.map(new BigDecimal("-10"), "  SAÚDE ").category());
        assertEquals(CategoryName.FOOD, mapper.map(new BigDecimal("-10"), "Alimentação").category());
    }

    @Test
    void categoriaDesconhecidaDeSaidaCaiEmOutrasDespesas() {
        var mapped = mapper.map(new BigDecimal("-9.90"), "Misc Fees");

        assertEquals(TransactionalType.EXPENSES, mapped.type());
        assertEquals(CategoryName.OTHER_EXPENSE, mapped.category());
    }

    @Test
    void categoriaDesconhecidaOuNulaDeEntradaCaiEmOutrasReceitas() {
        assertEquals(CategoryName.OTHER_INCOME, mapper.map(new BigDecimal("150"), null).category());
        assertEquals(CategoryName.OTHER_INCOME, mapper.map(new BigDecimal("150"), "???").category());
    }

    @Test
    void categoriaDeSaidaEmValorPositivoNaoVazaParaEntrada() {
        // estorno de mercado: e entrada, e FOOD so existe para saida
        var mapped = mapper.map(new BigDecimal("30"), "Groceries");

        assertEquals(TransactionalType.CASH_ENTRY, mapped.type());
        assertEquals(CategoryName.OTHER_INCOME, mapped.category());
    }

    @Test
    void categoriaDeEntradaEmValorNegativoNaoVazaParaSaida() {
        var mapped = mapper.map(new BigDecimal("-30"), "Salary");

        assertEquals(TransactionalType.EXPENSES, mapped.type());
        assertEquals(CategoryName.OTHER_EXPENSE, mapped.category());
    }

    @Test
    void oFallbackNuncaViolaARegraDeTipoDaCategoria() {
        assertEquals(TransactionalType.EXPENSES, mapper.map(new BigDecimal("-1"), "x").category().getTransactionalType());
        assertEquals(TransactionalType.CASH_ENTRY, mapper.map(new BigDecimal("1"), "x").category().getTransactionalType());
    }
}

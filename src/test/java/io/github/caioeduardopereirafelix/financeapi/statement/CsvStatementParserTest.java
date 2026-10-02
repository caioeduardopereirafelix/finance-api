package io.github.caioeduardopereirafelix.financeapi.statement;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvStatementParserTest {

    private final CsvStatementParser parser = new CsvStatementParser();

    @Test
    void leOFormatoBrasileiroComPontoEVirgula() {
        String csv = """
                Data;Descrição;Valor
                15/09/2026;Mercado Central;-1.234,56
                20/09/2026;Salário;3.500,00
                """;

        var parsed = parser.parse(csv, false);

        assertEquals(2, parsed.entries().size());
        assertEquals(new BigDecimal("-1234.56"), parsed.entries().get(0).amount());
        assertEquals("Mercado Central", parsed.entries().get(0).description());
        assertEquals(Instant.parse("2026-09-15T15:00:00Z"), parsed.entries().get(0).date());
        assertEquals(new BigDecimal("3500.00"), parsed.entries().get(1).amount());
    }

    @Test
    void pulaOCabecalhoDoBancoEIgnoraAColunaDeSaldo() {
        String csv = """
                Extrato Conta Corrente
                Conta;1234567
                Período;01/09/2026 a 30/09/2026

                Data Lançamento;Histórico;Descrição;Valor;Saldo
                15/09/2026;Pix enviado;FULANO DE TAL;-50,00;950,00
                16/09/2026;Pix recebido;BELTRANO;200,00;1.150,00
                """;

        var parsed = parser.parse(csv, false);

        assertEquals(2, parsed.entries().size());
        assertEquals("Pix enviado - FULANO DE TAL", parsed.entries().get(0).description());
        assertEquals(new BigDecimal("-50.00"), parsed.entries().get(0).amount());
        assertEquals(new BigDecimal("200.00"), parsed.entries().get(1).amount());
    }

    @Test
    void leColunasSeparadasDeDebitoECredito() {
        String csv = """
                Data;Histórico;Débito;Crédito
                15/09/2026;Compra;42,50;
                20/09/2026;Depósito;;100,00
                """;

        var parsed = parser.parse(csv, false);

        assertEquals(new BigDecimal("-42.50"), parsed.entries().get(0).amount());
        assertEquals(new BigDecimal("100.00"), parsed.entries().get(1).amount());
    }

    @Test
    void leCsvComVirgulaEDataIsoEInverteOSinalDoCartao() {
        String csv = """
                date,category,title,amount
                2026-09-15,restaurantes,Padaria Sol,25.90
                2026-09-16,transporte,Uber,-12.30
                """;

        var parsed = parser.parse(csv, true);

        assertEquals(new BigDecimal("-25.90"), parsed.entries().get(0).amount());
        assertEquals("restaurantes", parsed.entries().get(0).category());
        assertEquals(new BigDecimal("12.30"), parsed.entries().get(1).amount());
    }

    @Test
    void campoComAspasPodeTerOSeparadorEBomECrlfSaoTratados() {
        String csv = "\r\n" + "Data;Descricao;Valor\r\n"
                + "15/09/2026;\"Loja; Centro \"\"Matriz\"\"\";-10,00\r\n";

        var parsed = parser.parse(csv, false);

        assertEquals(1, parsed.entries().size());
        assertEquals("Loja; Centro \"Matriz\"", parsed.entries().get(0).description());
    }

    @Test
    void linhasRuinsSaoContadasEAsEmBrancoOuSemValorSaoIgnoradas() {
        String csv = """
                Data;Descrição;Valor
                15/09/2026;Boa;-1,00

                ;SALDO ANTERIOR;
                ontem;Data ruim;-2,00
                16/09/2026;Valor ruim;abc
                17/09/2026;Outra boa;-3,00
                """;

        var parsed = parser.parse(csv, false);

        assertEquals(2, parsed.entries().size());
        assertEquals(2, parsed.invalid());
        assertEquals(2, parsed.problems().size());
        assertTrue(parsed.problems().get(0).startsWith("Linha "));
    }

    @Test
    void semCabecalhoReconhecivelLancaExcecaoComOrientacao() {
        var erro = assertThrows(InvalidFieldException.class,
                () -> parser.parse("a;b;c\n1;2;3\n", false));

        assertTrue(erro.getMessage().contains("cabecalho"));
    }

    @Test
    void soCabecalhoSemLancamentosLancaExcecao() {
        assertThrows(InvalidFieldException.class, () -> parser.parse("Data;Descricao;Valor\n", false));
    }

    @Test
    void lancamentosIguaisNoMesmoDiaGanhamIdsDiferentesEEstaveis() {
        String csv = """
                Data;Descrição;Valor
                15/09/2026;Café;-5,00
                15/09/2026;Café;-5,00
                """;

        var first = parser.parse(csv, false);
        var second = parser.parse(csv, false);

        assertNotEquals(first.entries().get(0).id(), first.entries().get(1).id());
        assertEquals(first.entries().get(0).id(), second.entries().get(0).id());
        assertEquals(first.entries().get(1).id(), second.entries().get(1).id());
        assertTrue(first.entries().get(0).id().startsWith("csv:"));
    }
}

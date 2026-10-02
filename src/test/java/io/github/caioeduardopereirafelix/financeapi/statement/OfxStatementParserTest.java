package io.github.caioeduardopereirafelix.financeapi.statement;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfxStatementParserTest {

    private final OfxStatementParser parser = new OfxStatementParser();

    private static final String SGML = """
            OFXHEADER:100
            DATA:OFXSGML
            VERSION:102
            ENCODING:USASCII
            CHARSET:1252

            <OFX>
            <SIGNONMSGSRSV1><SONRS><STATUS><CODE>0<SEVERITY>INFO</STATUS><DTSERVER>20260930120000<LANGUAGE>POR</SONRS></SIGNONMSGSRSV1>
            <BANKMSGSRSV1><STMTTRNRS><TRNUID>1<STATUS><CODE>0<SEVERITY>INFO</STATUS>
            <STMTRS><CURDEF>BRL<BANKACCTFROM><BANKID>0260<ACCTID>12345-6<ACCTTYPE>CHECKING</BANKACCTFROM>
            <BANKTRANLIST><DTSTART>20260901<DTEND>20260930
            <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260915000000[-3:BRT]<TRNAMT>-42.50<FITID>A1<MEMO>MERCADO CENTRAL</STMTTRN>
            <STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20260920<TRNAMT>3500.00<FITID>A2<NAME>SALARIO<MEMO>CREDITO SALARIO</STMTTRN>
            </BANKTRANLIST>
            <LEDGERBAL><BALAMT>1000.00<DTASOF>20260930</LEDGERBAL>
            </STMTRS></STMTTRNRS></BANKMSGSRSV1>
            </OFX>
            """;

    @Test
    void leOFormatoSgmlComOsDoisLancamentos() {
        var parsed = parser.parse(SGML, false);

        assertEquals(2, parsed.entries().size());
        assertEquals(0, parsed.invalid());

        var debit = parsed.entries().get(0);
        assertEquals("ofx:12345-6:A1", debit.id());
        assertEquals(new BigDecimal("-42.50"), debit.amount());
        assertEquals("MERCADO CENTRAL", debit.description());
        assertEquals(Instant.parse("2026-09-15T03:00:00Z"), debit.date());

        var credit = parsed.entries().get(1);
        assertEquals("ofx:12345-6:A2", credit.id());
        assertEquals(new BigDecimal("3500.00"), credit.amount());
        assertEquals("SALARIO - CREDITO SALARIO", credit.description());
    }

    @Test
    void leOFormatoXmlComTagsFechadasEEntidades() {
        String xml = """
                <?xml version="1.0"?><OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS>
                <BANKACCTFROM><ACCTID>999</ACCTID></BANKACCTFROM>
                <BANKTRANLIST>
                <STMTTRN><TRNTYPE>DEBIT</TRNTYPE><DTPOSTED>20260915</DTPOSTED><TRNAMT>-10.00</TRNAMT><FITID>X1</FITID><NAME>BAR &amp; GRILL</NAME></STMTTRN>
                </BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
                """;

        var parsed = parser.parse(xml, false);

        assertEquals(1, parsed.entries().size());
        assertEquals("BAR & GRILL", parsed.entries().get(0).description());
        assertEquals("ofx:999:X1", parsed.entries().get(0).id());
    }

    @Test
    void inverterSinalTrocaEntradaPorSaida() {
        var parsed = parser.parse(SGML, true);

        assertEquals(new BigDecimal("42.50"), parsed.entries().get(0).amount());
        assertEquals(new BigDecimal("-3500.00"), parsed.entries().get(1).amount());
    }

    @Test
    void mesmoFitidEmContasDiferentesGeraIdsDiferentes() {
        String twoAccounts = """
                <OFX>
                <STMTRS><BANKACCTFROM><ACCTID>111</ACCTID></BANKACCTFROM><BANKTRANLIST>
                <STMTTRN><DTPOSTED>20260915<TRNAMT>-1.00<FITID>1<MEMO>A</STMTTRN>
                </BANKTRANLIST></STMTRS>
                <STMTRS><BANKACCTFROM><ACCTID>222</ACCTID></BANKACCTFROM><BANKTRANLIST>
                <STMTTRN><DTPOSTED>20260915<TRNAMT>-1.00<FITID>1<MEMO>A</STMTTRN>
                </BANKTRANLIST></STMTRS>
                </OFX>
                """;

        var parsed = parser.parse(twoAccounts, false);

        assertEquals(2, parsed.entries().size());
        assertNotEquals(parsed.entries().get(0).id(), parsed.entries().get(1).id());
    }

    @Test
    void semFitidUsaImpressaoDigitalEvitaColisaoEntreLancamentosIguais() {
        String noFitId = """
                <OFX><BANKTRANLIST>
                <STMTTRN><DTPOSTED>20260915<TRNAMT>-5.00<MEMO>CAFE</STMTTRN>
                <STMTTRN><DTPOSTED>20260915<TRNAMT>-5.00<MEMO>CAFE</STMTTRN>
                </BANKTRANLIST></OFX>
                """;

        var first = parser.parse(noFitId, false);
        var second = parser.parse(noFitId, false);

        assertNotEquals(first.entries().get(0).id(), first.entries().get(1).id());
        assertEquals(first.entries().get(0).id(), second.entries().get(0).id());
        assertEquals(first.entries().get(1).id(), second.entries().get(1).id());
        assertTrue(first.entries().get(0).id().startsWith("ofx:"));
    }

    @Test
    void lancamentoSemValorOuDataContaComoInvalidoEOsDemaisEntram() {
        String broken = """
                <OFX><BANKTRANLIST>
                <STMTTRN><DTPOSTED>20260915<FITID>1<MEMO>SEM VALOR</STMTTRN>
                <STMTTRN><DTPOSTED>lixo<TRNAMT>-1.00<FITID>2<MEMO>DATA RUIM</STMTTRN>
                <STMTTRN><DTPOSTED>20260916<TRNAMT>-2.00<FITID>3<MEMO>BOM</STMTTRN>
                </BANKTRANLIST></OFX>
                """;

        var parsed = parser.parse(broken, false);

        assertEquals(1, parsed.entries().size());
        assertEquals(2, parsed.invalid());
        assertEquals(2, parsed.problems().size());
        assertTrue(parsed.problems().get(0).startsWith("Lancamento 1"));
    }

    @Test
    void arquivoSemLancamentosLancaExcecao() {
        assertThrows(InvalidFieldException.class, () -> parser.parse("<OFX><BANKTRANLIST></BANKTRANLIST></OFX>", false));
    }

    @Test
    void lancamentoSemDescricaoRecebeUmTextoPadrao() {
        var parsed = parser.parse("<OFX><STMTTRN><DTPOSTED>20260915<TRNAMT>-1.00<FITID>1</STMTTRN></OFX>", false);

        assertEquals("Sem descricao", parsed.entries().get(0).description());
    }
}

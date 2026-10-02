package io.github.caioeduardopereirafelix.financeapi.statement;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatementParsersTest {

    private final StatementParsers parsers = new StatementParsers(new OfxStatementParser(), new CsvStatementParser());

    @Test
    void reconhecePeloConteudoOfxECsv() {
        byte[] ofx = "<OFX><STMTTRN><DTPOSTED>20260915<TRNAMT>-1.00<FITID>1<MEMO>X</STMTTRN></OFX>".getBytes(StandardCharsets.UTF_8);
        byte[] csv = "Data;Descricao;Valor\n15/09/2026;X;-1,00\n".getBytes(StandardCharsets.UTF_8);

        assertEquals("ofx::1", parsers.parse(ofx, false).entries().get(0).id());
        assertEquals(true, parsers.parse(csv, false).entries().get(0).id().startsWith("csv:"));
    }

    @Test
    void decodificaIso88591ComAcentosDeBancosBrasileiros() {
        byte[] csv = "Data;Descrição;Valor\n15/09/2026;PADARIA SÃO JOÃO;-8,00\n"
                .getBytes(Charset.forName("windows-1252"));

        assertEquals("PADARIA SÃO JOÃO", parsers.parse(csv, false).entries().get(0).description());
    }

    @Test
    void decodificaOfxEmWindows1252() {
        byte[] ofx = ("OFXHEADER:100\nCHARSET:1252\n\n<OFX><STMTTRN><DTPOSTED>20260915<TRNAMT>-1.00<FITID>1"
                + "<MEMO>AÇAÍ DA PRAÇA</STMTTRN></OFX>").getBytes(Charset.forName("windows-1252"));

        assertEquals("AÇAÍ DA PRAÇA", parsers.parse(ofx, false).entries().get(0).description());
    }

    @Test
    void aceitaUtf8ComBom() {
        byte[] csv = ("﻿Data;Descrição;Valor\n15/09/2026;Pão;-3,00\n").getBytes(StandardCharsets.UTF_8);

        assertEquals("Pão", parsers.parse(csv, false).entries().get(0).description());
    }

    @Test
    void arquivoVazioOuSoComEspacosEhRecusado() {
        assertThrows(InvalidFieldException.class, () -> parsers.parse(new byte[0], false));
        assertThrows(InvalidFieldException.class, () -> parsers.parse("   \n\n ".getBytes(StandardCharsets.UTF_8), false));
        assertThrows(InvalidFieldException.class, () -> parsers.parse(null, false));
    }
}

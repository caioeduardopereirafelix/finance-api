package io.github.caioeduardopereirafelix.financeapi.statement;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatementDateTest {

    private static LocalDate dayInSaoPaulo(Instant instant) {
        return instant.atZone(StatementDate.ZONE).toLocalDate();
    }

    @Test
    void ofxSoComDiaFicaAoMeioDiaDeSaoPaulo() {
        assertEquals(Instant.parse("2026-09-20T15:00:00Z"), StatementDate.ofx("20260920"));
    }

    @Test
    void ofxMeiaNoiteSemFusoViraMeioDia() {
        assertEquals(Instant.parse("2026-09-20T15:00:00Z"), StatementDate.ofx("20260920000000"));
    }

    @Test
    void ofxComFusoRespeitaOFusoInformado() {
        assertEquals(Instant.parse("2026-09-15T03:00:00Z"), StatementDate.ofx("20260915000000[-3:BRT]"));
        assertEquals(Instant.parse("2026-09-15T12:00:00Z"), StatementDate.ofx("20260915120000.000[0:GMT]"));
    }

    @Test
    void ofxNoFimDoDiaLocalContinuaNoMesmoDiaEmSaoPaulo() {
        Instant instant = StatementDate.ofx("20260930230000[-3:BRT]");

        assertEquals(LocalDate.of(2026, 9, 30), dayInSaoPaulo(instant));
    }

    @Test
    void ofxGmtQueCaiNoDiaAnteriorLocalEConvertidoCorretamente() {
        Instant instant = StatementDate.ofx("20261001010000[0:GMT]");

        assertEquals(LocalDate.of(2026, 9, 30), dayInSaoPaulo(instant));
    }

    @Test
    void csvAceitaIsoEDiaPrimeiro() {
        Instant expected = Instant.parse("2026-09-20T15:00:00Z");

        assertEquals(expected, StatementDate.csv("2026-09-20"));
        assertEquals(expected, StatementDate.csv("20/09/2026"));
        assertEquals(expected, StatementDate.csv("20-09-2026"));
        assertEquals(expected, StatementDate.csv("20.09.26"));
        assertEquals(expected, StatementDate.csv("2026-09-20T00:00:00-03:00".substring(0, 10)));
    }

    @Test
    void csvComHoraUsaAHoraDeSaoPaulo() {
        assertEquals(Instant.parse("2026-09-20T17:30:00Z"), StatementDate.csv("20/09/2026 14:30"));
        assertEquals(Instant.parse("2026-09-20T17:30:15Z"), StatementDate.csv("2026-09-20 14:30:15"));
    }

    @Test
    void dataInvalidaOuMuitoAntigaLancaExcecao() {
        assertThrows(DateTimeException.class, () -> StatementDate.csv("31/02/2026"));
        assertThrows(DateTimeException.class, () -> StatementDate.csv("ontem"));
        assertThrows(DateTimeException.class, () -> StatementDate.csv("01/01/1999"));
        assertThrows(DateTimeException.class, () -> StatementDate.ofx("abc"));
    }
}

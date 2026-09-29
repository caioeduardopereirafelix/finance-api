package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OccurredAtTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");   // UTC-3
    private static final Instant NOW = Instant.parse("2026-09-29T15:00:00Z");   // 29/09 12:00 em Sao Paulo

    private static LocalDate d(String iso) {
        return LocalDate.parse(iso);
    }

    @Test
    void semDataEmLancamentoNovoVaiParaAgora() {
        assertEquals(NOW, OccurredAt.resolve(null, null, SP, NOW));
    }

    @Test
    void semDataNaEdicaoMantemOQueJaEstava() {
        Instant current = Instant.parse("2026-09-10T18:30:00Z");
        assertEquals(current, OccurredAt.resolve(null, current, SP, NOW));
    }

    @Test
    void hojeVaiParaAgoraEGuardaAHoraReal() {
        assertEquals(NOW, OccurredAt.resolve(d("2026-09-29"), null, SP, NOW));
    }

    @Test
    void diaAnteriorFicaAoMeioDiaNoFusoDaAplicacao() {
        // 28/09 12:00 em Sao Paulo = 15:00 UTC: o mesmo dia em qualquer fuso proximo
        assertEquals(Instant.parse("2026-09-28T15:00:00Z"), OccurredAt.resolve(d("2026-09-28"), null, SP, NOW));
    }

    @Test
    void naoMudaAHoraDeQuemSoEditaOutraCoisaMantendoOMesmoDia() {
        Instant current = Instant.parse("2026-09-28T02:10:00Z");   // 27/09 23:10 em Sao Paulo
        assertEquals(current, OccurredAt.resolve(d("2026-09-27"), current, SP, NOW));
    }

    @Test
    void mudarODiaRecalculaEmVezDeManterAHoraAntiga() {
        Instant current = Instant.parse("2026-09-28T15:00:00Z");
        assertEquals(Instant.parse("2026-09-20T15:00:00Z"), OccurredAt.resolve(d("2026-09-20"), current, SP, NOW));
    }

    @Test
    void dataFuturaEhRecusada() {
        var e = assertThrows(InvalidFieldException.class, () -> OccurredAt.resolve(d("2026-09-30"), null, SP, NOW));
        assertEquals("occurredOn", e.getCampo());
    }

    @Test
    void perto_da_meia_noite_vale_o_dia_do_fuso_da_aplicacao_e_nao_o_de_UTC() {
        Instant now = Instant.parse("2026-09-30T02:00:00Z");   // 30/09 em UTC, mas ainda 29/09 23:00 em Sao Paulo

        assertEquals(now, OccurredAt.resolve(d("2026-09-29"), null, SP, now));   // hoje para o usuario
        assertThrows(InvalidFieldException.class, () -> OccurredAt.resolve(d("2026-09-30"), null, SP, now));
    }

    @Test
    void dataMuitoAntigaEhRecusada() {
        assertThrows(InvalidFieldException.class, () -> OccurredAt.resolve(d("1999-12-31"), null, SP, NOW));
        assertThrows(InvalidFieldException.class, () -> OccurredAt.resolve(d("0026-09-29"), null, SP, NOW));
    }

    @Test
    void primeiroDiaDeDoisMilEAceito() {
        assertEquals(Instant.parse("2000-01-01T14:00:00Z"), OccurredAt.resolve(d("2000-01-01"), null, SP, NOW));
    }
}

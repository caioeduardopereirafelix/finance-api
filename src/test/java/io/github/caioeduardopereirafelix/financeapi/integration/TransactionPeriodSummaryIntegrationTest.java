package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@TestPropertySource(properties = "bank.mock.enabled=true")
class TransactionPeriodSummaryIntegrationTest extends ApiIntegrationTestSupport {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private Account contaComTransacoesDoMock() throws Exception {
        var a = registerAndLogin();
        String body = mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"conn-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk());
        return a;
    }

    private String diasAtras(int dias) {
        return LocalDate.now(ZONE).minusDays(dias).toString();
    }

    @Test
    void semDatasOResumoContinuaCobrindoTudo() throws Exception {
        var a = contaComTransacoesDoMock();

        mockMvc.perform(get("/transaction/summary").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashEntry").value(5550.00))
                .andExpect(jsonPath("$.expenses").value(474.28))
                .andExpect(jsonPath("$.balance").value(5075.72));
    }

    @Test
    void resumoDeUmPeriodoSoSomaOQueOcorreuNele() throws Exception {
        var a = contaComTransacoesDoMock();
        mockMvc.perform(get("/transaction/summary")
                        .param("startDate", diasAtras(6)).param("endDate", diasAtras(0))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.cashEntry").value(0))
                .andExpect(jsonPath("$.expenses").value(424.48))
                .andExpect(jsonPath("$.balance").value(-424.48));
    }

    @Test
    void periodoSemTransacoesDevolveZeros() throws Exception {
        var a = contaComTransacoesDoMock();

        mockMvc.perform(get("/transaction/summary")
                        .param("startDate", diasAtras(400)).param("endDate", diasAtras(300))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.cashEntry").value(0))
                .andExpect(jsonPath("$.expenses").value(0))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void totalPorCategoriaVemDoMaiorParaOMenorComContagem() throws Exception {
        var a = contaComTransacoesDoMock();

        mockMvc.perform(get("/transaction/summary/by-category")
                        .param("startDate", diasAtras(30)).param("endDate", diasAtras(0))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].category").value("WAGE"))
                .andExpect(jsonPath("$[0].type").value("CASH_ENTRY"))
                .andExpect(jsonPath("$[0].total").value(5400.00))
                .andExpect(jsonPath("$[0].count").value(1))
                .andExpect(jsonPath("$[1].category").value("FOOD"))
                .andExpect(jsonPath("$[1].total").value(312.48));
    }

    @Test
    void totalPorCategoriaRespeitaOPeriodo() throws Exception {
        var a = contaComTransacoesDoMock();

        mockMvc.perform(get("/transaction/summary/by-category")
                        .param("startDate", diasAtras(6)).param("endDate", diasAtras(0))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.length()").value(3))   // alimentacao, saude e transporte
                .andExpect(jsonPath("$[0].category").value("FOOD"))
                .andExpect(jsonPath("$[1].category").value("HEALTH"))
                .andExpect(jsonPath("$[2].category").value("TRANSPORT"));
    }

    @Test
    void cadaUsuarioSoVeAsProprias() throws Exception {
        var a = contaComTransacoesDoMock();
        var b = registerAndLogin();

        mockMvc.perform(get("/transaction/summary/by-category").header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/transaction/summary").header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(jsonPath("$.balance").value(0));
        mockMvc.perform(get("/transaction/summary/by-category").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    void dataInicialDepoisDaFinalResponde422() throws Exception {
        var a = registerAndLogin();

        mockMvc.perform(get("/transaction/summary")
                        .param("startDate", "2026-09-30").param("endDate", "2026-09-01")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/transaction/summary/by-category")
                        .param("startDate", "2026-09-30").param("endDate", "2026-09-01")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void semTokenNaoAcessa() throws Exception {
        mockMvc.perform(get("/transaction/summary/by-category")).andExpect(status().isUnauthorized());
    }
}

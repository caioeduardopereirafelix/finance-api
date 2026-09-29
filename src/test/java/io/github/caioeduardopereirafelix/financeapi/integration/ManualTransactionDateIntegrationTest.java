package io.github.caioeduardopereirafelix.financeapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Data escolhida no lancamento manual. */
@TestPropertySource(properties = "bank.mock.enabled=true")
class ManualTransactionDateIntegrationTest extends ApiIntegrationTestSupport {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private static String dia(int diasAtras) {
        return LocalDate.now(ZONE).minusDays(diasAtras).toString();
    }

    private static String corpo(String descricao, String amount, String occurredOn) {
        String data = occurredOn == null ? "" : ",\"occurredOn\":\"" + occurredOn + "\"";
        return "{\"description\":\"" + descricao + "\",\"amount\":" + amount
                + ",\"type\":\"EXPENSES\",\"category\":\"FOOD\"" + data + "}";
    }

    private JsonNode criar(Account a, String body) throws Exception {
        String resposta = mockMvc.perform(post("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta);
    }

    private LocalDate diaDe(JsonNode transacao) {
        return Instant.parse(transacao.get("occurredAt").asText()).atZone(ZONE).toLocalDate();
    }

    @Test
    void semDataVaiParaAgora() throws Exception {
        var a = registerAndLogin();

        JsonNode t = criar(a, corpo("Cafe", "8.5", null));

        Instant quando = Instant.parse(t.get("occurredAt").asText());
        assertTrue(Duration.between(quando, Instant.now()).abs().toSeconds() < 30);
    }

    @Test
    void hojeTambemVaiParaAgora() throws Exception {
        var a = registerAndLogin();

        JsonNode t = criar(a, corpo("Cafe", "8.5", dia(0)));

        assertTrue(Duration.between(Instant.parse(t.get("occurredAt").asText()), Instant.now()).abs().toSeconds() < 30);
    }

    @Test
    void diaAnteriorGravaAquelaData() throws Exception {
        var a = registerAndLogin();

        JsonNode t = criar(a, corpo("Mercado de semana passada", "120", dia(10)));

        assertEquals(LocalDate.now(ZONE).minusDays(10), diaDe(t));
    }

    @Test
    void aDataEscolhidaDecideOPeriodoDoFiltroEDoResumo() throws Exception {
        var a = registerAndLogin();
        criar(a, corpo("Gasto antigo", "100", dia(10)));

        // nos ultimos 5 dias nao entra; nos ultimos 15, entra
        mockMvc.perform(get("/transaction/summary").param("startDate", dia(5)).param("endDate", dia(0))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.expenses").value(0));
        mockMvc.perform(get("/transaction/summary").param("startDate", dia(15)).param("endDate", dia(0))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.expenses").value(100));
        mockMvc.perform(get("/transaction").param("startDate", dia(11)).param("endDate", dia(9))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void dataFuturaResponde422() throws Exception {
        var a = registerAndLogin();
        String amanha = LocalDate.now(ZONE).plusDays(1).toString();

        mockMvc.perform(post("/transaction").header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Cafe", "8.5", amanha)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].field").value("occurredOn"));
    }

    @Test
    void dataMuitoAntigaResponde422() throws Exception {
        var a = registerAndLogin();

        mockMvc.perform(post("/transaction").header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Cafe", "8.5", "0026-09-29")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void dataEmFormatoInvalidoNaoEAceita() throws Exception {
        var a = registerAndLogin();

        mockMvc.perform(post("/transaction").header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Cafe", "8.5", "29/09/2026")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void editarADataMudaOInstante() throws Exception {
        var a = registerAndLogin();
        JsonNode t = criar(a, corpo("Cafe", "8.5", dia(2)));

        String resposta = mockMvc.perform(put("/transaction/" + t.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Cafe", "8.5", dia(7))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals(LocalDate.now(ZONE).minusDays(7), diaDe(objectMapper.readTree(resposta)));
    }

    @Test
    void editarSemMexerNaDataNaoAlteraOInstante() throws Exception {
        var a = registerAndLogin();
        JsonNode t = criar(a, corpo("Cafe", "8.5", dia(3)));
        String antes = t.get("occurredAt").asText();

        // sem occurredOn: a data nao muda
        String semData = mockMvc.perform(put("/transaction/" + t.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Cafe com pao", "9.5", null)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(antes, objectMapper.readTree(semData).get("occurredAt").asText());

        // com o mesmo dia: tambem nao
        String mesmoDia = mockMvc.perform(put("/transaction/" + t.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo("Cafe com pao", "9.5", dia(3))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(antes, objectMapper.readTree(mesmoDia).get("occurredAt").asText());
    }

    // ---------- transacao importada: a data e a do banco ----------

    private JsonNode tarifaImportada(Account a) throws Exception {
        String body = mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"conn-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk());
        String lista = mockMvc.perform(get("/transaction").param("description", "Tarifa")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(lista).get("content").get(0);
    }

    @Test
    void transacaoImportadaNaoAceitaMudarADataPeloPut() throws Exception {
        var a = registerAndLogin();
        JsonNode t = tarifaImportada(a);
        String outroDia = diaDe(t).minusDays(3).toString();

        mockMvc.perform(put("/transaction/" + t.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo("Tarifa desconhecida", "9.90", outroDia)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].field").value("occurredOn"));
    }

    @Test
    void transacaoImportadaAceitaOMesmoDiaEMudarSoACategoria() throws Exception {
        var a = registerAndLogin();
        JsonNode t = tarifaImportada(a);

        mockMvc.perform(put("/transaction/" + t.get("id").asText())
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tarifa desconhecida\",\"amount\":9.90,\"type\":\"EXPENSES\","
                                + "\"category\":\"BILLS\",\"occurredOn\":\"" + diaDe(t) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurredAt").value(t.get("occurredAt").asText()));
    }
}

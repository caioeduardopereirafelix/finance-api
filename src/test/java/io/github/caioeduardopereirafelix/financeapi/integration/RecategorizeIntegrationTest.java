package io.github.caioeduardopereirafelix.financeapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "bank.mock.enabled=true")
class RecategorizeIntegrationTest extends ApiIntegrationTestSupport {

    private void conectarESincronizar(Account a) throws Exception {
        String body = mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"conn-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk());
    }

    private JsonNode tarifas(Account a) throws Exception {
        String body = mockMvc.perform(get("/transaction").param("description", "Tarifa").param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("content");
    }

    private String idDaPrimeiraTarifa(Account a) throws Exception {
        return tarifas(a).get(0).get("id").asText();
    }

    private void recategorizar(Account a, String id, String category, boolean applyToSimilar) throws Exception {
        mockMvc.perform(patch("/transaction/" + id + "/category")
                .header(HttpHeaders.AUTHORIZATION, a.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"" + category + "\",\"applyToSimilar\":" + applyToSimilar + "}"));
    }

    private long quantasNaCategoria(Account a, String category) throws Exception {
        String body = mockMvc.perform(get("/transaction").param("description", "Tarifa").param("category", category)
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("totalElements").asLong();
    }

    @Test
    void trocaSoAquelaTransacaoQuandoNaoPedeParaAplicarNasParecidas() throws Exception {
        var a = registerAndLogin();
        conectarESincronizar(a);
        conectarESincronizar(a);
        String id = idDaPrimeiraTarifa(a);

        mockMvc.perform(patch("/transaction/" + id + "/category")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"BILLS\",\"applyToSimilar\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.transaction.id").value(id))
                .andExpect(jsonPath("$.transaction.category").value("BILLS"));

        org.junit.jupiter.api.Assertions.assertEquals(1, quantasNaCategoria(a, "BILLS"));
        org.junit.jupiter.api.Assertions.assertEquals(1, quantasNaCategoria(a, "OTHER_EXPENSE"));
    }

    @Test
    void aplicaNasParecidasJaImportadasEGuardaARegraParaAsProximas() throws Exception {
        var a = registerAndLogin();
        conectarESincronizar(a);
        conectarESincronizar(a);
        String id = idDaPrimeiraTarifa(a);

        mockMvc.perform(patch("/transaction/" + id + "/category")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"BILLS\",\"applyToSimilar\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        org.junit.jupiter.api.Assertions.assertEquals(2, quantasNaCategoria(a, "BILLS"));

        conectarESincronizar(a);
        org.junit.jupiter.api.Assertions.assertEquals(3, quantasNaCategoria(a, "BILLS"));
        org.junit.jupiter.api.Assertions.assertEquals(0, quantasNaCategoria(a, "OTHER_EXPENSE"));
    }

    @Test
    void aRegraNaoAtingeOutrosUsuarios() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();
        conectarESincronizar(a);
        conectarESincronizar(b);

        recategorizar(a, idDaPrimeiraTarifa(a), "BILLS", true);
        conectarESincronizar(b);

        org.junit.jupiter.api.Assertions.assertEquals(0, quantasNaCategoria(b, "BILLS"));
        org.junit.jupiter.api.Assertions.assertEquals(2, quantasNaCategoria(b, "OTHER_EXPENSE"));
    }

    @Test
    void escolherDeNovoAtualizaARegraEmVezDeDuplicar() throws Exception {
        var a = registerAndLogin();
        conectarESincronizar(a);
        String id = idDaPrimeiraTarifa(a);

        recategorizar(a, id, "BILLS", true);
        recategorizar(a, id, "HOUSING", true);
        conectarESincronizar(a);

        org.junit.jupiter.api.Assertions.assertEquals(2, quantasNaCategoria(a, "HOUSING"));
        org.junit.jupiter.api.Assertions.assertEquals(0, quantasNaCategoria(a, "BILLS"));
    }

    @Test
    void categoriaDeOutroTipoResponde422() throws Exception {
        var a = registerAndLogin();
        conectarESincronizar(a);

        mockMvc.perform(patch("/transaction/" + idDaPrimeiraTarifa(a) + "/category")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"WAGE\",\"applyToSimilar\":false}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void semCategoriaResponde422() throws Exception {
        var a = registerAndLogin();
        conectarESincronizar(a);

        mockMvc.perform(patch("/transaction/" + idDaPrimeiraTarifa(a) + "/category")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"applyToSimilar\":true}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void transacaoDeOutroUsuarioResponde404ENaoMuda() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();
        conectarESincronizar(a);
        String idDoA = idDaPrimeiraTarifa(a);

        mockMvc.perform(patch("/transaction/" + idDoA + "/category")
                        .header(HttpHeaders.AUTHORIZATION, b.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"BILLS\",\"applyToSimilar\":true}"))
                .andExpect(status().isNotFound());

        org.junit.jupiter.api.Assertions.assertEquals(1, quantasNaCategoria(a, "OTHER_EXPENSE"));
    }

    @Test
    void semTokenResponde401() throws Exception {
        mockMvc.perform(patch("/transaction/" + UUID.randomUUID() + "/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"BILLS\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void transacaoManualTambemPodeTrocarACategoria() throws Exception {
        var a = registerAndLogin();
        String body = mockMvc.perform(post("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Cafe\",\"amount\":8.5,\"type\":\"EXPENSES\",\"category\":\"FOOD\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(patch("/transaction/" + id + "/category")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"LEISURE\",\"applyToSimilar\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.category").value("LEISURE"));
    }

    @Test
    void transacaoImportadaNaoAceitaMudarValorNemDescricaoPeloPut() throws Exception {
        var a = registerAndLogin();
        conectarESincronizar(a);
        String id = idDaPrimeiraTarifa(a);

        mockMvc.perform(put("/transaction/" + id)
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tarifa desconhecida\",\"amount\":1.00,\"type\":\"EXPENSES\",\"category\":\"BILLS\"}"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(put("/transaction/" + id)
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Outro nome\",\"amount\":9.90,\"type\":\"EXPENSES\",\"category\":\"BILLS\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void transacaoImportadaAceitaMudarSoACategoriaPeloPut() throws Exception {
        var a = registerAndLogin();
        conectarESincronizar(a);
        String id = idDaPrimeiraTarifa(a);

        mockMvc.perform(put("/transaction/" + id)
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Tarifa desconhecida\",\"amount\":9.90,\"type\":\"EXPENSES\",\"category\":\"BILLS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("BILLS"));
    }

    @Test
    void transacaoManualContinuaEditavelPorInteiro() throws Exception {
        var a = registerAndLogin();
        String body = mockMvc.perform(post("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Cafe\",\"amount\":8.5,\"type\":\"EXPENSES\",\"category\":\"FOOD\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(put("/transaction/" + id)
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Cafe e pao\",\"amount\":12.00,\"type\":\"EXPENSES\",\"category\":\"FOOD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Cafe e pao"))
                .andExpect(jsonPath("$.amount").value(12.00));
    }
}

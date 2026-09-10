package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A promessa central da API e que cada usuario so enxerga o proprio dinheiro.
 * Estes testes cobrem exatamente isso.
 */
class TransactionIsolationIntegrationTest extends ApiIntegrationTestSupport {

    private String criarTransacao(Account account, String descricao, String valor,
                                  String tipo, String categoria) throws Exception {

        String body = mockMvc.perform(post("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"%s","amount":%s,"type":"%s","category":"%s"}
                                """.formatted(descricao, valor, tipo, categoria)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body).get("id").asText();
    }

    @Test
    void listagemDeveTrazerApenasAsTransacoesDoProprioUsuario() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();

        criarTransacao(a, "Salario do A", "3000.00", "CASH_ENTRY", "WAGE");
        criarTransacao(b, "Salario do B", "9999.00", "CASH_ENTRY", "WAGE");

        mockMvc.perform(get("/transaction").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Salario do A"));
    }

    @Test
    void naoDeveLerTransacaoDeOutroUsuario() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();

        String idDoB = criarTransacao(b, "Privado do B", "500.00", "EXPENSES", "FOOD");

        mockMvc.perform(get("/transaction/" + idDoB).header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void naoDeveAlterarNemApagarTransacaoDeOutroUsuario() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();

        String idDoB = criarTransacao(b, "Privado do B", "500.00", "EXPENSES", "FOOD");

        mockMvc.perform(put("/transaction/" + idDoB)
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"invadido","amount":1.00,"type":"EXPENSES","category":"FOOD"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/transaction/" + idDoB).header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isNotFound());

        // continua intacta para o dono
        mockMvc.perform(get("/transaction/" + idDoB).header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Privado do B"));
    }

    @Test
    void resumoDeveConsiderarApenasOProprioUsuario() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();

        criarTransacao(a, "Salario", "3000.00", "CASH_ENTRY", "WAGE");
        criarTransacao(a, "Mercado", "800.00", "EXPENSES", "FOOD");
        criarTransacao(b, "Salario do B", "9999.00", "CASH_ENTRY", "WAGE");

        mockMvc.perform(get("/transaction/summary").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashEntry").value(3000.00))
                .andExpect(jsonPath("$.expenses").value(800.00))
                .andExpect(jsonPath("$.balance").value(2200.00));
    }

    @Test
    void resumoDeUsuarioSemTransacoesDeveVirZerado() throws Exception {
        var novo = registerAndLogin();

        mockMvc.perform(get("/transaction/summary").header(HttpHeaders.AUTHORIZATION, novo.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashEntry").value(0))
                .andExpect(jsonPath("$.expenses").value(0))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void categoriaIncompativelComOTipoDeveSerRejeitada() throws Exception {
        var a = registerAndLogin();

        mockMvc.perform(post("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Salario","amount":100.00,"type":"CASH_ENTRY","category":"FOOD"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].field").value("category"));
    }

    @Test
    void createdDateDeveSerPreenchidoPelaAuditoria() throws Exception {
        var a = registerAndLogin();

        criarTransacao(a, "Salario", "3000.00", "CASH_ENTRY", "WAGE");

        mockMvc.perform(get("/transaction").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].createdDate").isNotEmpty());
    }
}

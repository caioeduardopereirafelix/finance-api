package io.github.caioeduardopereirafelix.financeapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.caioeduardopereirafelix.financeapi.bank.BankIntegrationException;
import io.github.caioeduardopereirafelix.financeapi.bank.pluggy.PluggyClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "bank.provider=pluggy",
        "bank.pluggy.client-id=id-teste",
        "bank.pluggy.client-secret=secret-teste"
})
class BankPluggyIntegrationTest extends ApiIntegrationTestSupport {


    private final String item = java.util.UUID.randomUUID().toString();

    @MockBean
    private PluggyClient pluggy;

    @Autowired
    private ObjectMapper json;

    @Test
    void fluxoCompletoComAPluggy() throws Exception {
        when(pluggy.createConnectToken(any())).thenReturn("connect-token-da-pluggy");
        when(pluggy.item(item)).thenReturn(json.readTree("{\"connector\":{\"name\":\"Banco Sandbox\"}}"));
        when(pluggy.accounts(item)).thenReturn(List.of(json.readTree("{\"id\":\"acc-1\",\"type\":\"BANK\"}")));
        when(pluggy.transactions(eq("acc-1"), any(LocalDate.class))).thenReturn(List.of(
                json.readTree("""
                        {"id":"p1","description":"Mercado do Bairro","amount":-88.10,"type":"DEBIT",
                         "date":"2026-09-20T14:00:00.000Z","status":"POSTED","category":"Groceries"}"""),
                json.readTree("""
                        {"id":"p2","description":"Salario","amount":4000,"type":"CREDIT",
                         "date":"2026-09-21T10:00:00.000Z","status":"POSTED","category":"Salary"}""")));

        var a = registerAndLogin();

        mockMvc.perform(post("/bank/connect-token").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("connect-token-da-pluggy"))
                .andExpect(jsonPath("$.provider").value("pluggy"));

        String body = mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"" + item + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institutionName").value("Banco Sandbox"))
                .andExpect(jsonPath("$.provider").value("pluggy"))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(body).get("id").asText();

        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2));

        mockMvc.perform(get("/transaction").param("category", "FOOD").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Mercado do Bairro"))
                .andExpect(jsonPath("$.content[0].source").value("BANK"));
    }

    @Test
    void itemQueNaoEUuidDeveSerRejeitadoSemChamarAPluggy() throws Exception {
        var a = registerAndLogin();

        mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"../accounts\"}"))
                .andExpect(status().isBadRequest());
    }

    private String conectarComPluggy(Account a) throws Exception {
        when(pluggy.item(item)).thenReturn(json.readTree("{\"connector\":{\"name\":\"Banco Sandbox\"}}"));
        String body = mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"" + item + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asText();
    }

    @Test
    void desconectarDeveRevogarNaPluggyEDepoisApagarLocalmente() throws Exception {
        var a = registerAndLogin();
        String id = conectarComPluggy(a);

        mockMvc.perform(delete("/bank/connections/" + id).header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isNoContent());

        verify(pluggy).deleteItem(item);
        mockMvc.perform(get("/bank/connections").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void seAPluggyFalharAoRevogarAConexaoContinuaExistindo() throws Exception {
        var a = registerAndLogin();
        String id = conectarComPluggy(a);
        doThrow(new BankIntegrationException(HttpStatus.BAD_GATEWAY, "A Pluggy recusou a operacao (HTTP 500)"))
                .when(pluggy).deleteItem(item);

        mockMvc.perform(delete("/bank/connections/" + id).header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("A Pluggy recusou a operacao (HTTP 500)"));
        mockMvc.perform(get("/bank/connections").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void reautorizarDevolveOTokenEOItemParaOWidgetAbrirNaConexao() throws Exception {
        var a = registerAndLogin();
        String id = conectarComPluggy(a);
        when(pluggy.createConnectToken(any(), eq(item))).thenReturn("token-de-atualizacao");

        mockMvc.perform(post("/bank/connections/" + id + "/update-token").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-de-atualizacao"))
                .andExpect(jsonPath("$.provider").value("pluggy"))
                .andExpect(jsonPath("$.externalId").value(item));
    }

    @Test
    void naoSePodeReautorizarConexaoDeOutroUsuario() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();
        String idDoA = conectarComPluggy(a);

        mockMvc.perform(post("/bank/connections/" + idDoA + "/update-token").header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(status().isNotFound());
    }
}

package io.github.caioeduardopereirafelix.financeapi.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.caioeduardopereirafelix.financeapi.bank.pluggy.PluggyClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "bank.provider=pluggy",
        "bank.pluggy.client-id=id-teste",
        "bank.pluggy.client-secret=secret-teste",
        "bank.pluggy.webhook-secret=segredo-de-teste"
})
class PluggyWebhookIntegrationTest extends ApiIntegrationTestSupport {

    private static final String WEBHOOK = "/webhooks/pluggy/segredo-de-teste";


    private final String item = UUID.randomUUID().toString();
    private final String outroItem = UUID.randomUUID().toString();

    @MockBean
    private PluggyClient pluggy;

    @Autowired
    private ObjectMapper json;

    private void stubItem(String itemId, String... transactionIds) throws Exception {
        when(pluggy.item(itemId)).thenReturn(json.readTree("{\"connector\":{\"name\":\"Banco Sandbox\"}}"));
        when(pluggy.accounts(itemId)).thenReturn(List.of(json.readTree("{\"id\":\"acc-" + itemId + "\",\"type\":\"BANK\"}")));
        var list = new java.util.ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        for (String id : transactionIds) {
            list.add(json.readTree("""
                    {"id":"%s","description":"Compra %s","amount":-10,"type":"DEBIT",
                     "date":"2026-09-20T14:00:00.000Z","status":"POSTED","category":"Groceries"}""".formatted(id, id)));
        }
        when(pluggy.transactions(eq("acc-" + itemId), any(LocalDate.class))).thenReturn(list);
    }

    private String conectar(Account a, String itemId) throws Exception {
        String body = mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"" + itemId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asText();
    }

    private void sincronizar(Account a, String connectionId) throws Exception {
        mockMvc.perform(post("/bank/connections/" + connectionId + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk());
    }

    private void webhook(String path, String payload) throws Exception {
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void segredoErradoResponde404ComoSeARotaNaoExistisse() throws Exception {
        mockMvc.perform(post("/webhooks/pluggy/errado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"transactions/deleted\",\"itemId\":\"" + item + "\",\"transactionIds\":[\"t1\"]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void transacoesExcluidasPelaPluggySaoApagadasSoDaConexaoDoItem() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();
        stubItem(item, "t1", "t2");
        stubItem(outroItem, "t1");
        String conexaoA = conectar(a, item);
        String conexaoB = conectar(b, outroItem);
        sincronizar(a, conexaoA);
        sincronizar(b, conexaoB);

        webhook(WEBHOOK, "{\"event\":\"transactions/deleted\",\"itemId\":\"" + item + "\",\"transactionIds\":[\"t1\"]}");

        mockMvc.perform(get("/transaction").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Compra t2"));
        mockMvc.perform(get("/transaction").header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void repetirOAvisoDeExclusaoNaoDaErro() throws Exception {
        var a = registerAndLogin();
        stubItem(item, "t1");
        sincronizar(a, conectar(a, item));
        String aviso = "{\"event\":\"transactions/deleted\",\"itemId\":\"" + item + "\",\"transactionIds\":[\"t1\"]}";

        webhook(WEBHOOK, aviso);
        webhook(WEBHOOK, aviso);

        mockMvc.perform(get("/transaction").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void transacoesCriadasDisparamUmaSincronizacaoEmSegundoPlano() throws Exception {
        var a = registerAndLogin();
        stubItem(item, "t1", "t2", "t3");
        conectar(a, item);

        webhook(WEBHOOK, "{\"event\":\"transactions/created\",\"itemId\":\"" + item + "\",\"accountId\":\"acc\"}");

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                mockMvc.perform(get("/transaction").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                        .andExpect(jsonPath("$.totalElements").value(3)));
    }

    @Test
    void itemDesconhecidoEAceitoSemFazerNada() throws Exception {
        webhook(WEBHOOK, "{\"event\":\"transactions/deleted\",\"itemId\":\"" + UUID.randomUUID()
                + "\",\"transactionIds\":[\"t1\"]}");
    }
}

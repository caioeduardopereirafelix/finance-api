package io.github.caioeduardopereirafelix.financeapi.bank.pluggy;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PluggyClientTest {

    private static final String BASE = "https://api.pluggy.test";

    private MockRestServiceServer server;
    private PluggyClient client;
    private AtomicReference<Instant> now;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        Clock clock = new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        client = new PluggyClient(builder.build(), "id-1", "secret-1", clock);
    }

    private void expectAuth(String apiKey) {
        server.expect(once(), requestTo(BASE + "/auth"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.clientId").value("id-1"))
                .andExpect(jsonPath("$.clientSecret").value("secret-1"))
                .andRespond(withSuccess("{\"apiKey\":\"" + apiKey + "\"}", MediaType.APPLICATION_JSON));
    }

    @Test
    void semCredenciaisNaoDeveSubir() {
        assertThrows(IllegalStateException.class,
                () -> new PluggyClient(RestClient.create(), "id", " ", Clock.systemUTC()));
    }

    @Test
    void deveAutenticarEEmitirConnectToken() {
        expectAuth("key-1");
        server.expect(once(), requestTo(BASE + "/connect_token"))
                .andExpect(header("X-API-KEY", "key-1"))
                .andExpect(jsonPath("$.options.clientUserId").value("user-42"))
                .andRespond(withSuccess("{\"accessToken\":\"connect-abc\"}", MediaType.APPLICATION_JSON));

        assertEquals("connect-abc", client.createConnectToken("user-42"));
        server.verify();
    }

    @Test
    void deveReaproveitarAApiKeyEntreChamadas() {
        expectAuth("key-1");   // uma unica autenticacao para as duas chamadas
        server.expect(once(), requestTo(BASE + "/items/item-1")).andExpect(header("X-API-KEY", "key-1"))
                .andRespond(withSuccess("{\"id\":\"item-1\"}", MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(BASE + "/items/item-2")).andExpect(header("X-API-KEY", "key-1"))
                .andRespond(withSuccess("{\"id\":\"item-2\"}", MediaType.APPLICATION_JSON));

        client.item("item-1");
        client.item("item-2");
        server.verify();
    }

    @Test
    void deveRenovarAApiKeyDepoisDoPrazo() {
        expectAuth("key-1");
        server.expect(once(), requestTo(BASE + "/items/a")).andExpect(header("X-API-KEY", "key-1"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        client.item("a");
        server.verify();
        server.reset();

        now.set(now.get().plus(PluggyClient.API_KEY_TTL).plusSeconds(1));

        expectAuth("key-2");
        server.expect(once(), requestTo(BASE + "/items/b")).andExpect(header("X-API-KEY", "key-2"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        client.item("b");
        server.verify();
    }

    @Test
    void deveRenovarAApiKeyEReTentarUmaVezQuandoARespostaForNaoAutorizado() {
        expectAuth("key-velha");
        server.expect(once(), requestTo(BASE + "/items/a")).andExpect(header("X-API-KEY", "key-velha"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        expectAuth("key-nova");
        server.expect(once(), requestTo(BASE + "/items/a")).andExpect(header("X-API-KEY", "key-nova"))
                .andRespond(withSuccess("{\"id\":\"a\"}", MediaType.APPLICATION_JSON));

        assertEquals("a", client.item("a").path("id").asText());
        server.verify();
    }

    @Test
    void erroDoServidorDaPluggyDevePropagar() {
        expectAuth("key-1");
        server.expect(once(), requestTo(BASE + "/items/a"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(HttpServerErrorException.class, () -> client.item("a"));
    }

    @Test
    void respostaDeAuthSemApiKeyDeveFalharComMensagemClara() {
        server.expect(once(), requestTo(BASE + "/auth"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        var e = assertThrows(IllegalStateException.class, () -> client.item("a"));
        assertEquals("Resposta da Pluggy sem o campo 'apiKey'", e.getMessage());
    }

    @Test
    void deveLerTodasAsPaginasDeTransacoes() {
        expectAuth("key-1");
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE + "/transactions")))
                .andExpect(queryParam("accountId", "acc-1"))
                .andExpect(queryParam("from", "2026-01-01"))
                .andExpect(queryParam("page", "1"))
                .andRespond(withSuccess("""
                        {"total":3,"totalPages":2,"page":1,"results":[{"id":"t1"},{"id":"t2"}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE + "/transactions")))
                .andExpect(queryParam("page", "2"))
                .andRespond(withSuccess("""
                        {"total":3,"totalPages":2,"page":2,"results":[{"id":"t3"}]}
                        """, MediaType.APPLICATION_JSON));

        List<JsonNode> all = client.transactions("acc-1", LocalDate.parse("2026-01-01"));

        assertEquals(List.of("t1", "t2", "t3"), all.stream().map(n -> n.path("id").asText()).toList());
        server.verify();
    }

    @Test
    void deveListarAsContasDoItem() {
        expectAuth("key-1");
        server.expect(once(), requestTo(org.hamcrest.Matchers.startsWith(BASE + "/accounts")))
                .andExpect(queryParam("itemId", "item-1"))
                .andRespond(withSuccess("""
                        {"results":[{"id":"a1","type":"BANK"},{"id":"a2","type":"CREDIT"}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals(2, client.accounts("item-1").size());
    }
}

package io.github.caioeduardopereirafelix.financeapi.bank.pluggy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Chamadas HTTP a API da Pluggy.
 *
 * A Pluggy usa dois niveis de credencial: o clientId/clientSecret (nossos, ficam
 * so no servidor) trocam por uma apiKey de curta duracao, que e o que vai no
 * header X-API-KEY das demais chamadas. A apiKey fica em cache e e renovada
 * antes de vencer, ou uma vez se a Pluggy responder 401/403.
 */
public class PluggyClient {

    /** A apiKey vale 2h na Pluggy; renovamos com folga. [confirmar na documentacao] */
    static final Duration API_KEY_TTL = Duration.ofMinutes(100);

    private static final int PAGE_SIZE = 500;
    private static final int MAX_PAGES = 200;   // trava de seguranca contra paginacao infinita

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient http;
    private final String clientId;
    private final String clientSecret;
    private final Clock clock;

    private String apiKey;
    private Instant apiKeyExpiresAt = Instant.MIN;

    public PluggyClient(RestClient http, String clientId, String clientSecret, Clock clock) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                    "PLUGGY_CLIENT_ID e PLUGGY_CLIENT_SECRET precisam estar definidos para usar a Pluggy");
        }
        this.http = http;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clock = clock;
    }

    /** Token que o front entrega ao widget. {@code clientUserId} liga a conexao ao nosso usuario. */
    public String createConnectToken(String clientUserId) {
        JsonNode response = authenticated(key -> http.post()
                .uri("/connect_token")
                .header("X-API-KEY", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json(Map.of("options", Map.of("clientUserId", clientUserId))))
                .retrieve()
                .body(JsonNode.class));
        return required(response, "accessToken");
    }

    public JsonNode item(String itemId) {
        return authenticated(key -> http.get()
                .uri("/items/{id}", itemId)
                .header("X-API-KEY", key)
                .retrieve()
                .body(JsonNode.class));
    }

    public List<JsonNode> accounts(String itemId) {
        JsonNode response = authenticated(key -> http.get()
                .uri(uri -> uri.path("/accounts").queryParam("itemId", itemId).build())
                .header("X-API-KEY", key)
                .retrieve()
                .body(JsonNode.class));
        return results(response);
    }

    /** Todas as paginas de movimentacoes da conta a partir de {@code from}. */
    public List<JsonNode> transactions(String accountId, LocalDate from) {
        List<JsonNode> all = new ArrayList<>();
        int page = 1;
        int totalPages = 1;

        while (page <= totalPages && page <= MAX_PAGES) {
            int current = page;
            JsonNode response = authenticated(key -> http.get()
                    .uri(uri -> uri.path("/transactions")
                            .queryParam("accountId", accountId)
                            .queryParam("from", from)
                            .queryParam("pageSize", PAGE_SIZE)
                            .queryParam("page", current)
                            .build())
                    .header("X-API-KEY", key)
                    .retrieve()
                    .body(JsonNode.class));
            all.addAll(results(response));
            totalPages = response == null ? 1 : response.path("totalPages").asInt(1);
            page++;
        }
        return all;
    }

    // ---------- autenticacao ----------

    private <T> T authenticated(Function<String, T> call) {
        try {
            return call.apply(currentApiKey());
        } catch (HttpClientErrorException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.isSameCodeAs(HttpStatus.UNAUTHORIZED) || status.isSameCodeAs(HttpStatus.FORBIDDEN)) {
                invalidateApiKey();
                return call.apply(currentApiKey());
            }
            throw e;
        }
    }

    private synchronized String currentApiKey() {
        if (apiKey == null || !clock.instant().isBefore(apiKeyExpiresAt)) {
            JsonNode response = http.post()
                    .uri("/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json(Map.of("clientId", clientId, "clientSecret", clientSecret)))
                    .retrieve()
                    .body(JsonNode.class);
            apiKey = required(response, "apiKey");
            apiKeyExpiresAt = clock.instant().plus(API_KEY_TTL);
        }
        return apiKey;
    }

    private synchronized void invalidateApiKey() {
        apiKey = null;
    }

    // ---------- corpo e resposta ----------

    /**
     * Corpo como bytes, para a requisicao levar Content-Length. Com um objeto, o
     * RestClient manda em "chunked", que nem todo servidor/proxy aceita bem.
     */
    private static byte[] json(Object body) {
        try {
            return MAPPER.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Nao foi possivel montar o corpo da requisicao", e);
        }
    }

    private static List<JsonNode> results(JsonNode response) {
        List<JsonNode> list = new ArrayList<>();
        if (response != null && response.path("results").isArray()) {
            response.path("results").forEach(list::add);
        }
        return list;
    }

    private static String required(JsonNode response, String field) {
        String value = response == null ? null : response.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Resposta da Pluggy sem o campo '" + field + "'");
        }
        return value;
    }
}

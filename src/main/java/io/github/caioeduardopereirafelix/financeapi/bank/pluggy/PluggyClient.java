package io.github.caioeduardopereirafelix.financeapi.bank.pluggy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.caioeduardopereirafelix.financeapi.bank.BankIntegrationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
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

    private static final int MAX_PAGES = 200;   // trava de seguranca contra paginacao infinita
    private static final String TRANSACTIONS_PATH = "/v2/transactions";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient http;
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final Clock clock;

    private String apiKey;
    private Instant apiKeyExpiresAt = Instant.MIN;

    public PluggyClient(RestClient http, String baseUrl, String clientId, String clientSecret, Clock clock) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                    "PLUGGY_CLIENT_ID e PLUGGY_CLIENT_SECRET precisam estar definidos para usar a Pluggy");
        }
        this.http = http;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
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

    /**
     * Apaga o item na Pluggy, o que revoga a autorizacao e remove os dados la.
     * Um item que ja nao existe (404) conta como sucesso, para poder repetir a operacao.
     */
    public void deleteItem(String itemId) {
        try {
            authenticated(key -> http.delete()
                    .uri("/items/{id}", itemId)
                    .header("X-API-KEY", key)
                    .retrieve()
                    .toBodilessEntity());
        } catch (BankIntegrationException e) {
            if (e.getCause() instanceof RestClientResponseException r && r.getStatusCode().value() == 404) {
                return;
            }
            throw e;
        }
    }

    public List<JsonNode> accounts(String itemId) {
        JsonNode response = authenticated(key -> http.get()
                .uri(uri -> uri.path("/accounts").queryParam("itemId", itemId).build())
                .header("X-API-KEY", key)
                .retrieve()
                .body(JsonNode.class));
        return results(response);
    }

    /**
     * Todas as movimentacoes da conta a partir de {@code from}, pela API v2 (a v1,
     * GET /transactions, foi desativada e responde 410).
     *
     * A v2 pagina por cursor: cada resposta traz "next", uma query string pronta
     * (?accountId=...&after=...) que se anexa como esta ao caminho do endpoint, ou
     * null na ultima pagina. O "next" e usado sem recodificar, porque o cursor e
     * base64 e recodifica-lo mudaria o valor.
     */
    public List<JsonNode> transactions(String accountId, LocalDate from) {
        List<JsonNode> all = new ArrayList<>();
        String query = "?accountId=" + UriUtils.encodeQueryParam(accountId, StandardCharsets.UTF_8) + "&dateFrom=" + from;
        String previous = null;

        for (int page = 1; page <= MAX_PAGES; page++) {
            URI uri = URI.create(baseUrl + TRANSACTIONS_PATH + query);
            JsonNode response = authenticated(key -> http.get()
                    .uri(uri)
                    .header("X-API-KEY", key)
                    .retrieve()
                    .body(JsonNode.class));
            all.addAll(results(response));

            String next = response == null || response.path("next").isNull() ? null : response.path("next").asText(null);
            if (next == null || next.isBlank() || next.equals(previous)) {
                break;   // ultima pagina (ou o cursor nao andou: nao deixa girar em circulo)
            }
            if (!next.startsWith("?")) {
                // Sem isso, um "next" estranho poderia mudar o caminho ou o servidor da chamada.
                throw new IllegalStateException("Cursor de paginacao da Pluggy em formato inesperado");
            }
            previous = next;
            // o filtro de data pode nao vir dentro do cursor; sem ele voltaria ate 12 meses
            query = next.contains("dateFrom=") ? next : next + "&dateFrom=" + from;
        }
        return all;
    }

    // ---------- autenticacao ----------

    private <T> T authenticated(Function<String, T> call) {
        try {
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
        } catch (RestClientResponseException e) {
            throw new BankIntegrationException(HttpStatus.BAD_GATEWAY,
                    "A Pluggy recusou a operacao (HTTP " + e.getStatusCode().value() + ")" + detail(e), e);
        }
    }

    /**
     * O que a Pluggy disse, para quem usa a tela (e quem le o log) saber o motivo em
     * vez de um "falhou" generico. So o campo "message" do erro, sem cabecalhos nem
     * o que enviamos, entao nao carrega credencial.
     */
    private static String detail(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body == null || body.isBlank()) {
            return "";
        }
        String message = body;
        try {
            JsonNode parsed = MAPPER.readTree(body);
            if (parsed.path("message").isTextual()) {
                message = parsed.path("message").asText();
            }
        } catch (JsonProcessingException notJson) {
            // corpo que nao e JSON: usa o texto como veio
        }
        message = message.replaceAll("\\s+", " ").trim();
        if (message.length() > 200) {
            message = message.substring(0, 200) + "…";
        }
        return message.isEmpty() ? "" : ": " + message;
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

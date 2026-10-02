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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class PluggyClient {

    static final Duration API_KEY_TTL = Duration.ofMinutes(100);

    private static final int MAX_PAGES = 200;
    private static final String TRANSACTIONS_PATH = "/v2/transactions";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient http;
    private final String baseUrl;
    private final String webhookUrl;
    private final String clientId;
    private final String clientSecret;
    private final Clock clock;

    private String apiKey;
    private Instant apiKeyExpiresAt = Instant.MIN;

    public PluggyClient(RestClient http, String baseUrl, String clientId, String clientSecret, Clock clock) {
        this(http, baseUrl, null, clientId, clientSecret, clock);
    }

    public PluggyClient(RestClient http, String baseUrl, String webhookUrl, String clientId, String clientSecret,
                        Clock clock) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException(
                    "PLUGGY_CLIENT_ID e PLUGGY_CLIENT_SECRET precisam estar definidos para usar a Pluggy");
        }
        this.http = http;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.webhookUrl = webhookUrl == null || webhookUrl.isBlank() ? null : webhookUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clock = clock;
    }

    public String createConnectToken(String clientUserId) {
        return createConnectToken(clientUserId, null);
    }

    public String createConnectToken(String clientUserId, String itemId) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("clientUserId", clientUserId);
        if (webhookUrl != null) {
            options.put("webhookUrl", webhookUrl);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        if (itemId != null) {
            body.put("itemId", itemId);
        }
        body.put("options", options);

        JsonNode response = authenticated(key -> http.post()
                .uri("/connect_token")
                .header("X-API-KEY", key)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json(body))
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
                break;
            }
            if (!next.startsWith("?")) {
                throw new IllegalStateException("Cursor de paginacao da Pluggy em formato inesperado");
            }
            previous = next;
            query = next.contains("dateFrom=") ? next : next + "&dateFrom=" + from;
        }
        return all;
    }

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

package io.github.caioeduardopereirafelix.financeapi.bank.pluggy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@Configuration
@ConditionalOnExpression("!'${bank.pluggy.client-id:}'.isBlank()")
public class PluggyConfig {

    @Bean
    PluggyClient pluggyClient(RestClient.Builder builder,
                              @Value("${bank.pluggy.base-url:https://api.pluggy.ai}") String baseUrl,
                              @Value("${bank.pluggy.client-id}") String clientId,
                              @Value("${bank.pluggy.client-secret:}") String clientSecret,
                              @Value("${bank.pluggy.webhook-secret:}") String webhookSecret,
                              @Value("${bank.pluggy.webhook-base-url:}") String webhookBaseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));

        RestClient http = builder.baseUrl(baseUrl).requestFactory(factory).build();
        return new PluggyClient(http, baseUrl, webhookUrl(webhookBaseUrl, webhookSecret), clientId, clientSecret,
                Clock.systemUTC());
    }

    static String webhookUrl(String baseUrl, String secret) {
        if (baseUrl == null || baseUrl.isBlank() || secret == null || secret.isBlank()) {
            return null;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/webhooks/pluggy/" + secret;
    }

    @Bean
    PluggyBankProvider pluggyBankProvider(PluggyClient client,
                                          @Value("${bank.pluggy.include-credit-cards:false}") boolean includeCreditCards,
                                          @Value("${app.zone:America/Sao_Paulo}") ZoneId zone) {
        return new PluggyBankProvider(client, includeCreditCards, zone, Clock.systemUTC());
    }
}

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

/**
 * Liga a Pluggy quando PLUGGY_CLIENT_ID esta definido. Sem ele, nada disto
 * existe e o restante da aplicacao nao percebe a diferenca.
 */
@Configuration
@ConditionalOnExpression("!'${bank.pluggy.client-id:}'.isBlank()")
public class PluggyConfig {

    @Bean
    PluggyClient pluggyClient(RestClient.Builder builder,
                              @Value("${bank.pluggy.base-url:https://api.pluggy.ai}") String baseUrl,
                              @Value("${bank.pluggy.client-id}") String clientId,
                              @Value("${bank.pluggy.client-secret:}") String clientSecret) {
        // Sem timeout, uma Pluggy lenta seguraria a requisicao (e o agendador) indefinidamente.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));

        RestClient http = builder.baseUrl(baseUrl).requestFactory(factory).build();
        return new PluggyClient(http, baseUrl, clientId, clientSecret, Clock.systemUTC());
    }

    @Bean
    PluggyBankProvider pluggyBankProvider(PluggyClient client,
                                          @Value("${bank.pluggy.include-credit-cards:false}") boolean includeCreditCards,
                                          @Value("${app.zone:America/Sao_Paulo}") ZoneId zone) {
        return new PluggyBankProvider(client, includeCreditCards, zone, Clock.systemUTC());
    }
}

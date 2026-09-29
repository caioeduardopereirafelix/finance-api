package io.github.caioeduardopereirafelix.financeapi.bank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Localiza o provedor pelo nome, e sabe qual deve ser usado em novas conexoes. */
@Component
public class BankProviders {

    private final Map<String, BankProvider> byName;
    private final String defaultName;

    public BankProviders(List<BankProvider> providers, @Value("${bank.provider:mock}") String defaultName) {
        this.byName = providers.stream().collect(Collectors.toMap(BankProvider::name, Function.identity()));
        this.defaultName = defaultName;
    }

    public BankProvider forNewConnections() {
        return named(defaultName);
    }

    public BankProvider named(String name) {
        BankProvider provider = byName.get(name);
        if (provider == null) {
            throw new BankIntegrationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Nenhum provedor bancario configurado com o nome '" + name + "'");
        }
        return provider;
    }
}

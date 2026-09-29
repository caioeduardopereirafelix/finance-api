package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.BankConnectionStatus;
import io.github.caioeduardopereirafelix.financeapi.repository.BankConnectionRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankConnectionService {

    private final BankConnectionRepository connections;
    private final TransactionRepository transactions;
    private final BankProviders providers;

    /** Token do widget e o nome do provedor, para o front saber qual widget abrir. */
    public ConnectToken createConnectToken(User user) {
        BankProvider provider = providers.forNewConnections();
        return new ConnectToken(provider.createConnectToken(user.getId().toString()), provider.name());
    }

    public record ConnectToken(String token, String provider) {
    }

    @Transactional
    public BankConnection connect(User user, String externalId) {
        BankProvider provider = providers.forNewConnections();

        if (connections.existsByProviderAndExternalId(provider.name(), externalId)) {
            throw new BankIntegrationException(HttpStatus.CONFLICT, "Esta conexao bancaria ja esta cadastrada");
        }

        ExternalConnection described;
        try {
            described = provider.describeConnection(externalId, user.getId().toString());
        } catch (BankIntegrationException e) {
            log.warn("Conexao {} recusada: {}", externalId, e.getMessage());
            throw e;   // o provedor ja disse o que houve (ex.: identificador invalido)
        } catch (RuntimeException e) {
            log.warn("Nao foi possivel confirmar a conexao {}: {}", externalId, e.toString());
            throw new BankIntegrationException(HttpStatus.BAD_GATEWAY,
                    "Nao foi possivel confirmar a conexao no provedor bancario", e);
        }

        BankConnection connection = new BankConnection();
        connection.setUser(user);
        connection.setProvider(provider.name());
        connection.setExternalId(externalId);
        connection.setInstitutionName(described.institutionName());
        connection.setStatus(BankConnectionStatus.ACTIVE);
        connection.setCreatedAt(Instant.now());
        return connections.save(connection);
    }

    @Transactional(readOnly = true)
    public List<BankConnection> list(User user) {
        return connections.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * @param deleteImported apaga tambem as transacoes importadas dessa conexao.
     *                       Sem isso elas ficam no historico, sem vinculo.
     */
    @Transactional
    public void disconnect(User user, UUID id, boolean deleteImported) {
        BankConnection connection = connections.findByIdAndUser(id, user)
                .orElseThrow(() -> new BankIntegrationException(HttpStatus.NOT_FOUND, "Conexao bancaria nao encontrada"));

        if (deleteImported) {
            transactions.deleteByBankConnection(connection);
        } else {
            transactions.detachFromConnection(connection);
        }
        connections.delete(connection);
    }
}

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

    /** Token para reautorizar uma conexao; {@code externalId} e o que o widget precisa para abrir nela. */
    public record ReauthToken(String token, String provider, String externalId) {
    }

    @Transactional(readOnly = true)
    public ReauthToken createUpdateToken(User user, UUID id) {
        BankConnection connection = connections.findByIdAndUser(id, user)
                .orElseThrow(() -> new BankIntegrationException(HttpStatus.NOT_FOUND, "Conexao bancaria nao encontrada"));

        BankProvider provider = providers.named(connection.getProvider());
        String token = provider.createUpdateToken(connection.getExternalId(), user.getId().toString());
        return new ReauthToken(token, provider.name(), connection.getExternalId());
    }

    /**
     * Revoga todas as conexoes do usuario nos provedores (usado antes de apagar a conta).
     *
     * Falha do provedor interrompe: sem isso a conta sumiria deixando a autorizacao viva la, e
     * nao haveria mais como revogar. Repetir e seguro (item ja apagado = sucesso). Provedor que
     * nao esta configurado neste servidor e pulado com aviso, senao a conta nunca poderia ser apagada.
     */
    public void revokeAll(User user) {
        for (BankConnection connection : connections.findByUserOrderByCreatedAtDesc(user)) {
            BankProvider provider;
            try {
                provider = providers.named(connection.getProvider());
            } catch (BankIntegrationException e) {
                log.warn("Conexao {} nao foi revogada: {}", connection.getId(), e.getMessage());
                continue;
            }
            try {
                provider.disconnect(connection.getExternalId());
            } catch (BankIntegrationException e) {
                log.warn("Nao foi possivel revogar a conexao {}: {}", connection.getId(), e.getMessage());
                throw e;
            } catch (RuntimeException e) {
                log.warn("Nao foi possivel revogar a conexao {}: {}", connection.getId(), e.toString());
                throw new BankIntegrationException(HttpStatus.BAD_GATEWAY,
                        "Nao foi possivel revogar a autorizacao no provedor bancario. Tente de novo.", e);
            }
        }
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

        // Primeiro o provedor: se a revogacao falhar, nada local muda e a pessoa tenta de novo.
        // Se o local falhar depois, tentar outra vez e seguro (o provedor ja sem o item = sucesso).
        try {
            providers.named(connection.getProvider()).disconnect(connection.getExternalId());
        } catch (BankIntegrationException e) {
            log.warn("Nao foi possivel revogar a conexao {} no provedor: {}", connection.getId(), e.getMessage());
            throw e;
        } catch (RuntimeException e) {
            log.warn("Nao foi possivel revogar a conexao {} no provedor: {}", connection.getId(), e.toString());
            throw new BankIntegrationException(HttpStatus.BAD_GATEWAY,
                    "Nao foi possivel revogar a autorizacao no provedor bancario. Tente de novo.", e);
        }

        if (deleteImported) {
            transactions.deleteByBankConnection(connection);
        } else {
            transactions.detachFromConnection(connection);
        }
        connections.delete(connection);
    }
}

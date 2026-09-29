package io.github.caioeduardopereirafelix.financeapi.bank;

import java.time.Instant;
import java.util.List;

/**
 * Contrato com um agregador de Open Finance (Pluggy, Belvo, ...).
 *
 * A aplicacao so conhece esta interface. Trocar de agregador e escrever uma
 * nova implementacao e registrar o nome dela em bank.provider.
 */
public interface BankProvider {

    /** Nome estavel, gravado em bank_connections.provider. */
    String name();

    /**
     * Token de curta duracao que o front usa para abrir o widget de conexao do
     * agregador. O usuario autoriza o banco la dentro; as credenciais dele
     * nunca chegam ao nosso backend.
     */
    String createConnectToken(String userReference);

    /**
     * Confirma que a conexao existe no provedor e devolve os dados dela.
     *
     * @param userReference o mesmo valor passado a {@link #createConnectToken}; o provedor
     *                      deve recusar (403) uma conexao que ele sabe ser de outro usuario
     */
    ExternalConnection describeConnection(String externalId, String userReference);

    /** Movimentacoes da conexao a partir de {@code since}. */
    List<ExternalTransaction> fetchTransactions(String externalId, Instant since);
}

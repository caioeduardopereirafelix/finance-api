package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction>/*JpaSpecificationExecutor -> dynamic search*/ {

    Optional<Transaction> findByIdAndUser(UUID id, User user);

    boolean existsByUserAndExternalId(User user, String externalId);

    /** Apaga o que foi importado de uma conexao (usado ao desconectar, a pedido do usuario). */
    void deleteByBankConnection(BankConnection bankConnection);

    /**
     * Desvincula as transacoes da conexao, mantendo o historico. Feito no codigo
     * e nao so pelo ON DELETE SET NULL da migration, para o comportamento nao
     * depender do banco em uso.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Transaction t set t.bankConnection = null where t.bankConnection = :connection")
    void detachFromConnection(@Param("connection") BankConnection connection);

    /**
     * Soma os valores por tipo direto no banco, em vez de carregar todas as
     * transacoes do usuario na memoria so para somar.
     */
    @Query("""
            select t.type as type, sum(t.amount) as total
            from Transaction t
            where t.user = :user
            group by t.type
            """)
    List<TransactionSummaryProjection> summarizeByType(@Param("user") User user);
}

package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction>/*JpaSpecificationExecutor -> dynamic search*/ {

    Optional<Transaction> findByIdAndUser(UUID id, User user);

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

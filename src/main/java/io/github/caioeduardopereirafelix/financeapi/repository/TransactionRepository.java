package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUser(UUID id, User user);

    boolean existsByUserAndExternalId(User user, String externalId);

    List<Transaction> findByUserAndSourceNotAndType(User user, TransactionSource source, TransactionalType type);

    @Query("select t.externalId from Transaction t where t.user = :user and t.externalId in :ids")
    List<String> findExistingExternalIds(@Param("user") User user, @Param("ids") Collection<String> ids);

    void deleteByBankConnection(BankConnection bankConnection);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Transaction t set t.bankConnection = null where t.bankConnection = :connection")
    void detachFromConnection(@Param("connection") BankConnection connection);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Transaction t where t.bankConnection = :connection and t.externalId in :externalIds")
    int deleteImported(@Param("connection") BankConnection connection,
                       @Param("externalIds") Collection<String> externalIds);

    @Query("""
            select t.type as type, sum(t.amount) as total
            from Transaction t
            where t.user = :user
              and t.occurredAt >= :from
              and t.occurredAt < :before
            group by t.type
            """)
    List<TransactionSummaryProjection> summarizeByType(@Param("user") User user,
                                                       @Param("from") Instant from,
                                                       @Param("before") Instant before);

    @Query("""
            select t.category as category, t.type as type, sum(t.amount) as total, count(t) as count
            from Transaction t
            where t.user = :user
              and t.occurredAt >= :from
              and t.occurredAt < :before
            group by t.category, t.type
            order by sum(t.amount) desc
            """)
    List<CategoryTotalProjection> totalsByCategory(@Param("user") User user,
                                                   @Param("from") Instant from,
                                                   @Param("before") Instant before);
}

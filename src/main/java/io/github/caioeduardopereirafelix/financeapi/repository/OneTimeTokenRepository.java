package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.OneTimeToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@NoRepositoryBean
public interface OneTimeTokenRepository<T extends OneTimeToken> extends JpaRepository<T, UUID> {

    @Query("select t from #{#entityName} t join fetch t.user where t.tokenHash = :tokenHash")
    Optional<T> findByTokenHash(@Param("tokenHash") String tokenHash);

    Optional<T> findFirstByUserOrderByCreatedAtDesc(User user);

    @Transactional
    @Modifying
    @Query("delete from #{#entityName} t where t.id = :id")
    int consume(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query("delete from #{#entityName} t where t.user = :user")
    void deleteByUser(@Param("user") User user);

    @Transactional
    @Modifying
    @Query("delete from #{#entityName} t where t.expiresAt < :limit")
    void deleteExpiredBefore(@Param("limit") Instant limit);
}

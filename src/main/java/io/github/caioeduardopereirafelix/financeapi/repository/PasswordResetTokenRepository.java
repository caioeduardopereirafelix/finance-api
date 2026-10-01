package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.PasswordResetToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Query("select t from PasswordResetToken t join fetch t.user where t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    boolean existsByUserAndCreatedAtAfter(User user, Instant limit);

    @Transactional
    @Modifying
    @Query("delete from PasswordResetToken t where t.id = :id")
    int consume(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query("delete from PasswordResetToken t where t.user = :user")
    void deleteByUser(@Param("user") User user);

    @Transactional
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :limit")
    void deleteExpiredBefore(@Param("limit") Instant limit);
}

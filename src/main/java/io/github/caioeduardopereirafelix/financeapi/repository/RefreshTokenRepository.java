package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.RefreshToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Traz o usuario no mesmo select: o fluxo de refresh usa os dados dele
     * fora da transacao, e a associacao e LAZY.
     */
    @Query("select rt from RefreshToken rt join fetch rt.user where rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHash(@Param("tokenHash") String tokenHash);

    void deleteByUser(User user);

    void deleteByExpiresAtBefore(Instant limit);
}

package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.BankConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankConnectionRepository extends JpaRepository<BankConnection, UUID> {

    List<BankConnection> findByUserOrderByCreatedAtDesc(User user);

    Optional<BankConnection> findByIdAndUser(UUID id, User user);

    boolean existsByProviderAndExternalId(String provider, String externalId);

    List<BankConnection> findByStatus(BankConnectionStatus status);
}

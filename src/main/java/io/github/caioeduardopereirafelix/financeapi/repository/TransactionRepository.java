package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction>/*JpaSpecificationExecutor -> dynamic search*/ {

    List<Transaction> findByUser(User user);

    Page<Transaction> findByUser(User user, Pageable pageable);

    Optional<Transaction> findByIdAndUser(UUID id, User user);
}

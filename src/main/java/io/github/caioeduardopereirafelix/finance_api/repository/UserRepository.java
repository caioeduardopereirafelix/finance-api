package io.github.caioeduardopereirafelix.finance_api.repository;

import io.github.caioeduardopereirafelix.finance_api.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}

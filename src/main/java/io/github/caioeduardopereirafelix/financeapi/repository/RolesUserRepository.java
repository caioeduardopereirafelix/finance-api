package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.RolesUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolesUserRepository extends JpaRepository<RolesUser, Integer> {
    Optional<RolesUser> findByName(String role);
}

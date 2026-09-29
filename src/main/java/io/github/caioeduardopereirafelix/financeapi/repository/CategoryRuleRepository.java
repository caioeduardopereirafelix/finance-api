package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.CategoryRule;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRuleRepository extends JpaRepository<CategoryRule, UUID> {

    List<CategoryRule> findByUser(User user);

    Optional<CategoryRule> findByUserAndMatchKeyAndType(User user, String matchKey, TransactionalType type);
}

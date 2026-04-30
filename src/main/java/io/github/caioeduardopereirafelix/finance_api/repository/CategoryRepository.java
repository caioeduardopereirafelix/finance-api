package io.github.caioeduardopereirafelix.finance_api.repository;

import io.github.caioeduardopereirafelix.finance_api.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}

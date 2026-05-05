package io.github.caioeduardopereirafelix.financeapi.repository;

import io.github.caioeduardopereirafelix.financeapi.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}

package io.github.caioeduardopereirafelix.finance_api.model.entity;

import io.github.caioeduardopereirafelix.finance_api.model.CategoryName;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class Category {

    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private CategoryName name;



}

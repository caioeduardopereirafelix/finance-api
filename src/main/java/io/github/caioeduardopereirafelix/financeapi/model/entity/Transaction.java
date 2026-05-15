package io.github.caioeduardopereirafelix.financeapi.model.entity;

import io.github.caioeduardopereirafelix.financeapi.config.AuditingClass;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
public class Transaction extends AuditingClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private String description;

    @Column
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column
    private TransactionalType type; // ENTRADA ou DESPESA

    //Relacionamento com usuário
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private CategoryName category;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;
}
package io.github.caioeduardopereirafelix.financeapi.model.entity;

import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

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

    // 🔗 Relacionamento com usuário
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private User user;

    // 🔗 Relacionamento com categoria
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private Category category;
}
package io.github.caioeduardopereirafelix.finance_api.model.entity;

import io.github.caioeduardopereirafelix.finance_api.model.TransactionalType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private User user;

    // 🔗 Relacionamento com categoria
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;
}
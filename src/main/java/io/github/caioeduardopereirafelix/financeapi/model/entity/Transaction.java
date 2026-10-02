package io.github.caioeduardopereirafelix.financeapi.model.entity;

import io.github.caioeduardopereirafelix.financeapi.config.AuditingClass;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionSource;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(exclude = {"user", "bankConnection"})
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
    private TransactionalType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private CategoryName category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionSource source = TransactionSource.MANUAL;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_connection_id")
    private BankConnection bankConnection;

    @PrePersist
    void defaultOccurredAt() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}

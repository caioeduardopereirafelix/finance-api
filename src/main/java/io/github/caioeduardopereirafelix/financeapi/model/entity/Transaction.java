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
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private TransactionalType type; // ENTRADA ou DESPESA

    //Relacionamento com usuário
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private CategoryName category;

    /** De onde veio o lancamento: digitado pelo usuario ou importado do banco. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionSource source = TransactionSource.MANUAL;

    /** Id da transacao no provedor, prefixado por ele. Nulo nos lancamentos manuais. */
    @Column(name = "external_id")
    private String externalId;

    /**
     * Quando o gasto ocorreu. E a data que vale para listar, ordenar e filtrar.
     * Nos importados vem do banco; nos manuais e o momento do lancamento.
     * createdDate continua sendo so o momento em que o registro entrou aqui.
     */
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

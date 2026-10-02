package io.github.caioeduardopereirafelix.financeapi.model.entity;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "category_rules")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class CategoryRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "match_key", nullable = false, length = 120)
    private String matchKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionalType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoryName category;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

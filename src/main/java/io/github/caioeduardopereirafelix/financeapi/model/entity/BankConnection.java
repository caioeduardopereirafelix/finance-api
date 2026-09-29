package io.github.caioeduardopereirafelix.financeapi.model.entity;

import io.github.caioeduardopereirafelix.financeapi.model.enums.BankConnectionStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Vinculo entre um usuario e uma conta/instituicao no provedor bancario.
 *
 * Guardamos apenas o identificador que o provedor devolve. Credenciais do
 * banco nunca passam por esta aplicacao: quem as recebe e o provedor.
 */
@Entity
@Table(name = "bank_connections")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(exclude = "user")
public class BankConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "institution_name")
    private String institutionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BankConnectionStatus status;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

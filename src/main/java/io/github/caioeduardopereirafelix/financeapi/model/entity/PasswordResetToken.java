package io.github.caioeduardopereirafelix.financeapi.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "password_reset_tokens")
@SuperBuilder
@NoArgsConstructor
public class PasswordResetToken extends OneTimeToken {
}

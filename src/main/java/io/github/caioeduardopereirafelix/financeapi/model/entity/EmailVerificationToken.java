package io.github.caioeduardopereirafelix.financeapi.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "email_verification_tokens")
@SuperBuilder
@NoArgsConstructor
public class EmailVerificationToken extends OneTimeToken {
}

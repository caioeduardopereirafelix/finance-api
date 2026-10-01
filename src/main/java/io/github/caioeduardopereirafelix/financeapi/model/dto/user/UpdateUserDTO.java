package io.github.caioeduardopereirafelix.financeapi.model.dto.user;

import io.github.caioeduardopereirafelix.financeapi.config.EmailPolicy;
import io.github.caioeduardopereirafelix.financeapi.config.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Todos os campos sao opcionais; o que vier preenchido precisa ser valido. */
public record UpdateUserDTO(
        String name,
        @Email(message = "Email invalido", regexp = EmailPolicy.PATTERN)
        String email,
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
                message = "Senha deve ter no mínimo 8 caracteres")
        String password,
        @Size(max = PasswordPolicy.MAX_LENGTH, message = "Senha atual muito longa")
        String currentPassword) {

    public UpdateUserDTO {
        email = EmailPolicy.normalize(email);
    }
}

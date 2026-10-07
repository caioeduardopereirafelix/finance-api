package io.github.caioeduardopereirafelix.financeapi.model.dto.user;

import io.github.caioeduardopereirafelix.financeapi.config.EmailPolicy;
import io.github.caioeduardopereirafelix.financeapi.config.NamePolicy;
import io.github.caioeduardopereirafelix.financeapi.config.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
        @NotBlank(message = "O nome não pode ficar vazio")
        @Size(max = NamePolicy.MAX_LENGTH, message = "O nome deve ter no máximo {max} caracteres")
        String name,

        @NotBlank(message = "O e-mail não pode ficar vazio")
        @Email(message = "E-mail inválido", regexp = EmailPolicy.PATTERN)
        String email,

        @NotBlank(message = "A senha não pode ficar vazia")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
                message = "A senha deve ter entre {min} e {max} caracteres")
        String password) {

    public CreateUserDTO {
        email = EmailPolicy.normalize(email);
    }
}

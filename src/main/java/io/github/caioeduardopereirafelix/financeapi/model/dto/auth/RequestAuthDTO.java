package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import io.github.caioeduardopereirafelix.financeapi.config.EmailPolicy;
import io.github.caioeduardopereirafelix.financeapi.config.NamePolicy;
import io.github.caioeduardopereirafelix.financeapi.config.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestAuthDTO(
        @NotBlank(message = "O e-mail não pode ficar vazio")
        @Email(message = "E-mail inválido", regexp = EmailPolicy.PATTERN)
        String email,

        @NotBlank(message = "O nome não pode ficar vazio")
        @Size(max = NamePolicy.MAX_LENGTH, message = "O nome deve ter no máximo {max} caracteres")
        String user,

        @NotBlank(message = "A senha não pode ficar vazia")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
                message = "A senha deve ter entre {min} e {max} caracteres")
        String password) {

    public RequestAuthDTO {
        email = EmailPolicy.normalize(email);
    }
}

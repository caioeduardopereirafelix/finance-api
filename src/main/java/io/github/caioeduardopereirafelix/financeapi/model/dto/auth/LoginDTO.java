package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import io.github.caioeduardopereirafelix.financeapi.config.EmailPolicy;
import io.github.caioeduardopereirafelix.financeapi.config.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDTO(
        @NotBlank(message = "O e-mail não pode ficar vazio")
        @Email(message = "E-mail inválido")
        String email,
        @NotBlank(message = "A senha não pode ficar vazia")
        @Size(max = PasswordPolicy.MAX_LENGTH, message = "A senha é longa demais")
        String password
) {

    public LoginDTO {
        email = EmailPolicy.normalize(email);
    }
}

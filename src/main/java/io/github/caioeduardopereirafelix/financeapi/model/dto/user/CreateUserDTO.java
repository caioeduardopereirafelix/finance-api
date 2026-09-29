package io.github.caioeduardopereirafelix.financeapi.model.dto.user;

import io.github.caioeduardopereirafelix.financeapi.config.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
        @NotNull
        @Size(max = 20, min = 1, message =  "Nome deve ter no máximo 100 caracteres e no minimo 1")
        String name,
        @NotBlank(message = "Email nao pode ser vazio")
        @Email(message = "Email invalido")
        String email,
        @NotBlank(message = "Senha pode estar vazia")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
                message = "Senha deve ter no mínimo 8 caracteres")
        String password) {
}

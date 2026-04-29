package io.github.caioeduardopereirafelix.finance_api.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserDTO(
        @NotNull
        @Size(max = 20, min = 1, message = "Tamanho Invalido")
        String firstName,
        @Email(message = "Email invalido")
        String email,
        @NotNull
        String password) {
}

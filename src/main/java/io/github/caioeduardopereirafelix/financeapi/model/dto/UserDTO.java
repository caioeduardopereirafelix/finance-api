package io.github.caioeduardopereirafelix.financeapi.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserDTO(
        @NotNull
        @Size(max = 20, min = 1, message =  "Nome deve ter no máximo 100 caracteres e no minimo 1")
        String firstName,
        @NotBlank(message = "Email nao pode ser vazio")
        @Email(message = "Email invalido")
        String email,
        @NotBlank(message = "Senha pode estar vazia")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String password) {
}

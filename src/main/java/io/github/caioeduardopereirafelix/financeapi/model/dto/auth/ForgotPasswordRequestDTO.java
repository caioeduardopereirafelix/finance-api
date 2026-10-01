package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDTO(
        @NotBlank(message = "email it cannot  be empty")
        @Email(message = "invalid email format")
        String email) {
}

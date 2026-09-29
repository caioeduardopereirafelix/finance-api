package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import io.github.caioeduardopereirafelix.financeapi.config.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDTO(
        @NotBlank(message = "email it cannot  be empty")
        @Email(message = "invalid email format")
        String email,
        @NotBlank(message = "password it cannot  be empty")
        @Size(max = PasswordPolicy.MAX_LENGTH, message = "password is too long")
        String password
) {
}

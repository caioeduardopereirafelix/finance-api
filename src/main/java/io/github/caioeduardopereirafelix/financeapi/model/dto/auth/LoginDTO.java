package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginDTO(
        @NotBlank(message = "email it cannot  be empty")
        @Email(message = "invalid email format")
        String email,
        @NotBlank(message = "password it cannot  be empty")
        @Size(min = 5, max = 100, message = "password must contain at least 5 characters")
        String password
) {
}

package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import io.github.caioeduardopereirafelix.financeapi.config.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDTO(
        @NotBlank(message = "token it cannot  be empty")
        @Size(max = 200, message = "token is too long")
        String token,
        @NotBlank(message = "password it cannot  be empty")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
                message = "password must contain at least 8 characters")
        String password) {
}

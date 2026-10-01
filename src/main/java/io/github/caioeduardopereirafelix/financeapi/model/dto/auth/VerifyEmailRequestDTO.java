package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequestDTO(
        @NotBlank(message = "token it cannot  be empty")
        @Size(max = 200, message = "token is too long")
        String token) {
}

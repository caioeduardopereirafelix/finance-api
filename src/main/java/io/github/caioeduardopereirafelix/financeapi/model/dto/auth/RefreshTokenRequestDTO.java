package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(
        @NotBlank(message = "refreshToken cannot be empty")
        String refreshToken) {
}

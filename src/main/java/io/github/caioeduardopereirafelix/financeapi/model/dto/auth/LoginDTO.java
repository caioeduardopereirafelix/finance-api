package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginDTO(
        @NotBlank
        String email,
        @NotBlank
        String senha
) {
}

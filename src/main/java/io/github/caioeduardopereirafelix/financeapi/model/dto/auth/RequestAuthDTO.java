package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import jakarta.validation.constraints.Email;

public record RequestAuthDTO(
        @Email
        String email,
        String senha) {
}

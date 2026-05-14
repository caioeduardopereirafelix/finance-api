package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record RequestAuthDTO(
        @NotNull
        @Email
        String email,
        @NotNull
        String user,
        @NotNull
        String senha) {
}

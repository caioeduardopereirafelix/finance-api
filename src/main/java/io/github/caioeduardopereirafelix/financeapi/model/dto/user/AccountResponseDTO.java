package io.github.caioeduardopereirafelix.financeapi.model.dto.user;

public record AccountResponseDTO(
        String name,
        String email,
        boolean emailVerified) {
}

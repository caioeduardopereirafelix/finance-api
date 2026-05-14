package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

public record ResponseAuthDTO(
        String token,
        Long expiresIn
) {
}

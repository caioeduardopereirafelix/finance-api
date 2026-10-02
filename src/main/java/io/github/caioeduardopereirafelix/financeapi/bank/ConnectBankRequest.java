package io.github.caioeduardopereirafelix.financeapi.bank;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConnectBankRequest(
        @NotBlank(message = "externalId cannot be empty")
        @Size(max = 120, message = "externalId is too long")
        String externalId) {
}

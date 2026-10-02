package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.model.enums.BankConnectionStatus;

import java.time.Instant;
import java.util.UUID;

public record BankConnectionResponse(
        UUID id,
        String provider,
        String institutionName,
        BankConnectionStatus status,
        Instant lastSyncedAt,
        Instant createdAt) {

    public static BankConnectionResponse from(BankConnection c) {
        return new BankConnectionResponse(c.getId(), c.getProvider(), c.getInstitutionName(),
                c.getStatus(), c.getLastSyncedAt(), c.getCreatedAt());
    }
}

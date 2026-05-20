package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

import java.math.BigDecimal;

public record SummaryResponseDTO(
        BigDecimal cashEntry,
        BigDecimal expense,
        BigDecimal balance
) {
}

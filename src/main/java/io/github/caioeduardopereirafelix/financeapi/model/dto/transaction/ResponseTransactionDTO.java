package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionSource;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ResponseTransactionDTO(
        UUID id,
        String description,
        BigDecimal amount,
        CategoryName category,
        TransactionalType type,
        /** Quando o gasto ocorreu: a data que a tela deve mostrar. */
        Instant occurredAt,
        TransactionSource source,
        /** Quando o registro entrou no sistema. */
        Instant createdDate) {
}

package io.github.caioeduardopereirafelix.financeapi.bank;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Movimentacao no formato do provedor, ja normalizada para tipos nossos.
 *
 * @param amount   com sinal: negativo e saida, positivo e entrada
 * @param category categoria do provedor, em texto livre (pode ser nula)
 */
public record ExternalTransaction(
        String id,
        String description,
        BigDecimal amount,
        Instant date,
        String category) {
}

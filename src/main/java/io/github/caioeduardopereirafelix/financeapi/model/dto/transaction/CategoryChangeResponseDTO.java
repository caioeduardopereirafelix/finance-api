package io.github.caioeduardopereirafelix.financeapi.model.dto.transaction;

/** A transacao ja com a nova categoria e quantas mudaram no total (a propria mais as parecidas). */
public record CategoryChangeResponseDTO(ResponseTransactionDTO transaction, int updated) {
}

package io.github.caioeduardopereirafelix.financeapi.exceptions;

public class TransactionNotFound extends RuntimeException {
    public TransactionNotFound(String message) {
        super(message);
    }
}

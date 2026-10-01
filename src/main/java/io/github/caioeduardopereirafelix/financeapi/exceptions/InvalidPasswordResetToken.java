package io.github.caioeduardopereirafelix.financeapi.exceptions;

public class InvalidPasswordResetToken extends RuntimeException {
    public InvalidPasswordResetToken(String message) {
        super(message);
    }
}

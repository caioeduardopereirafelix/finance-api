package io.github.caioeduardopereirafelix.financeapi.exceptions;

public class InvalidEmailVerificationToken extends RuntimeException {
    public InvalidEmailVerificationToken(String message) {
        super(message);
    }
}

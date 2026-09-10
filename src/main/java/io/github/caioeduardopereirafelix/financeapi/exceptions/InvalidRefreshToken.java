package io.github.caioeduardopereirafelix.financeapi.exceptions;

public class InvalidRefreshToken extends RuntimeException {
    public InvalidRefreshToken(String message) {
        super(message);
    }
}

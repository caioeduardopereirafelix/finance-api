package io.github.caioeduardopereirafelix.financeapi.exceptions;

public class RegistrationDuplicated extends RuntimeException {
    public RegistrationDuplicated(String message) {
        super(message);
    }
}

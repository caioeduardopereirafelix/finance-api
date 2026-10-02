package io.github.caioeduardopereirafelix.financeapi.bank;

import org.springframework.http.HttpStatus;

public class BankIntegrationException extends RuntimeException {

    private final HttpStatus status;

    public BankIntegrationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public BankIntegrationException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

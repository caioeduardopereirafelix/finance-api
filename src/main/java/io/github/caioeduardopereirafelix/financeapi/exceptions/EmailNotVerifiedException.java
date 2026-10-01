package io.github.caioeduardopereirafelix.financeapi.exceptions;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException() {
        super("Confirme seu e-mail para conectar um banco");
    }
}

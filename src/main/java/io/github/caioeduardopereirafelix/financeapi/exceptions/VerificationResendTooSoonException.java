package io.github.caioeduardopereirafelix.financeapi.exceptions;

public class VerificationResendTooSoonException extends RuntimeException {

    private final long retryAfterSeconds;

    public VerificationResendTooSoonException(long retryAfterSeconds) {
        super("Um e-mail de confirmação acabou de ser enviado. Aguarde " + retryAfterSeconds + " segundos para pedir outro.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

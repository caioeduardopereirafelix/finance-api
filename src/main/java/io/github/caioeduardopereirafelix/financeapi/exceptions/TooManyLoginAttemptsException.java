package io.github.caioeduardopereirafelix.financeapi.exceptions;

public class TooManyLoginAttemptsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyLoginAttemptsException(long retryAfterSeconds) {
        super("Muitas tentativas de login. Tente novamente em " + minutes(retryAfterSeconds) + ".");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    private static String minutes(long seconds) {
        long min = (seconds + 59) / 60;
        return min <= 1 ? "1 minuto" : min + " minutos";
    }
}

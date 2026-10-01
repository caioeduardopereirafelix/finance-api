package io.github.caioeduardopereirafelix.financeapi.config;

import java.util.Locale;

public final class EmailPolicy {

    public static final String PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$";

    private EmailPolicy() {
    }

    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}

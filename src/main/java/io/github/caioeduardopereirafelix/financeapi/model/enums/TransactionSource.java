package io.github.caioeduardopereirafelix.financeapi.model.enums;

public enum TransactionSource {
    MANUAL,
    BANK,
    FILE;

    public boolean imported() {
        return this != MANUAL;
    }
}

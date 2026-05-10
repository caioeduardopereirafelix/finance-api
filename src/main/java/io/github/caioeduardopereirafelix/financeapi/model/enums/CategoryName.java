package io.github.caioeduardopereirafelix.financeapi.model.enums;


public enum CategoryName {

    //CASH_ENTRY
    WAGE(TransactionalType.CASH_ENTRY),
    EXTRA_INCOME(TransactionalType.CASH_ENTRY),

    //EXPENSES
    FOOD(TransactionalType.EXPENSES),
    LEISURE(TransactionalType.EXPENSES),
    HOUSING(TransactionalType.EXPENSES),
    HEALTH(TransactionalType.EXPENSES),
    TRANSPORT(TransactionalType.EXPENSES),
    INVESTMENTS(TransactionalType.EXPENSES),
    BILLS(TransactionalType.EXPENSES);

    private final TransactionalType transactionalType;

    CategoryName(TransactionalType transactionalType) {
        this.transactionalType = transactionalType;
    }

    public TransactionalType getTransactionalType() {
        return transactionalType;
    }
}

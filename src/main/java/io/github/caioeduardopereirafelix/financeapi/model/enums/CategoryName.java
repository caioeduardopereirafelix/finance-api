package io.github.caioeduardopereirafelix.financeapi.model.enums;


public enum CategoryName {

    //CASH_ENTRY
    WAGE(TransactionalType.CASH_ENTRY),
    EXTRA_INCOME(TransactionalType.CASH_ENTRY),

    //EXPENSES
    FOOD(TransactionalType.EXPENSES),
    LEISURE(TransactionalType.EXPENSES),
    HOUSING(TransactionalType.EXPENSES),
    HEALT(TransactionalType.EXPENSES),
    TRANSPORT(TransactionalType.EXPENSES),
    INVESTMENTS(TransactionalType.EXPENSES),
    COUNTS(TransactionalType.EXPENSES);

    private final TransactionalType categoryType;

    CategoryName(TransactionalType categoryType) {
        this.categoryType = categoryType;
    }
}

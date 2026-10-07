package io.github.caioeduardopereirafelix.financeapi.statement;

public interface StatementParser {

    boolean supports(String head);

    ParsedStatement parse(String text, boolean invertSign);
}

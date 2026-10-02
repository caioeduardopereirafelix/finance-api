package io.github.caioeduardopereirafelix.financeapi.statement;

import java.util.List;

public record ParsedStatement(List<StatementEntry> entries, int invalid, List<String> problems) {
}

package io.github.caioeduardopereirafelix.financeapi.exceptions;

import java.util.List;

public record ResponseError(int status, String error, List<ErrorField> fieldsError) {
}

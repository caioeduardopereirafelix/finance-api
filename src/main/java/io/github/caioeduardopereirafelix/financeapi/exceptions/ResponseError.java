package io.github.caioeduardopereirafelix.financeapi.exceptions;

import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public record ResponseError(int status, String error, List<FieldError> fieldsError) {

    public static ResponseError errorStandard(String msg){
        return new ResponseError(HttpStatus.BAD_REQUEST.value(), msg, List.of());
    }
}

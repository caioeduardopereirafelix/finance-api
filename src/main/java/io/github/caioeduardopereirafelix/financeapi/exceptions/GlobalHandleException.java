package io.github.caioeduardopereirafelix.financeapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@ControllerAdvice
@RestControllerAdvice
public class GlobalHandleException {

    @ExceptionHandler(EmailInvalidException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseError exception(EmailInvalidException exception){
        return new ResponseError(HttpStatus.CONFLICT.value(), HttpStatus.CONFLICT, exception.getMessage(), LocalDateTime.now());
    }
}


package io.github.caioeduardopereirafelix.financeapi.exceptions;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ResponseError(int status, HttpStatus error, String mensage, LocalDateTime timestamp) {
}

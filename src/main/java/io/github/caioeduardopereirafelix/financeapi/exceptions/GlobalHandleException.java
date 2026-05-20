package io.github.caioeduardopereirafelix.financeapi.exceptions;

import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@RestControllerAdvice
public class GlobalHandleException {

    @ExceptionHandler(EmailAlreadyExistException.class)
    public ResponseEntity<ResponseError> emailExcetionHandle(EmailAlreadyExistException exception){
        var error=
                new ResponseError(HttpStatus.CONFLICT.value(), exception.getMessage(), List.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidFieldException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseError handleCampoInvalidoExceptions(InvalidFieldException e){
        return new ResponseError(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Erro Validacacao", List.of(new ErrorField(e.getCampo(),e.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseError handleMethodArgumentNotValidException(MethodArgumentNotValidException e){
        List<ErrorField> listaDeErros = e.getFieldErrors()
                .stream()
                .map(fl -> new ErrorField(fl.getField(), fl.getDefaultMessage()))
                .collect(Collectors.toList());
        return new ResponseError(HttpStatus.UNPROCESSABLE_ENTITY.value(), "erro de validacao", listaDeErros);
    }

    @ExceptionHandler(RegistrationDuplicated.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseError handleRegistrationDuplicated(RegistrationDuplicated e){
        return new ResponseError(HttpStatus.BAD_REQUEST.value(), "", List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseError handleBadCredentialsException(BadCredentialsException e){
        return new ResponseError(HttpStatus.UNAUTHORIZED.value(), "Invalid Email or Password", List.of());
    }



}


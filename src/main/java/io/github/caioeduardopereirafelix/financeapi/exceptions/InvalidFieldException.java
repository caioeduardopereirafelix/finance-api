package io.github.caioeduardopereirafelix.financeapi.exceptions;

import lombok.Getter;

@Getter
public class InvalidFieldException extends RuntimeException {

    private String campo;

    public InvalidFieldException(String campo, String msg){
        super(msg);
        this.campo = campo;
    }
}
package io.github.caioeduardopereirafelix.financeapi.service.validator;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.exceptions.EmailAlreadyExistException;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository repository;

    public void validatePassword(String senha){
        if (senha.isBlank()) throw new InvalidFieldException("Password","Password cannot be blank");
    }

    public void validateEmail(String email){
        if (repository.existsByEmail(email)) throw new EmailAlreadyExistException("Email already exists");
    }

    public void validateName(String name){
        if (name.isBlank()) throw new InvalidFieldException("Name","Name cannot be blank");
    }
}

package io.github.caioeduardopereirafelix.financeapi.service.validator;

import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.exceptions.EmailAlreadyExistException;
import io.github.caioeduardopereirafelix.financeapi.exceptions.RegistrationDuplicated;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository repository;

    public void validatePassword(String senha){
        if (senha.isBlank()) {
            throw new InvalidFieldException("Password","Password cannot be blank");
        }
    }

    public void validateEmail(String email){
        if (repository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistException("Email already exists");
        }
    }

    public void validateName(String name){
        if (name.isBlank()) {
            throw new InvalidFieldException("Name","Name cannot be blank");
        }
    }

    public void validate(User user) {
        if (existUser(user)){
            throw new RegistrationDuplicated("Autor já cadastrado");
        }
    }


    private boolean existUser(User user){

        Optional<User> userFound =
                repository.findByEmail(user.getEmail());

        if (user.getId() == null){
            return userFound.isPresent();
        }

        return !user.getId().equals(userFound.get().getId()) && userFound.isPresent();
    }
}

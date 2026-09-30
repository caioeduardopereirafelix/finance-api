package io.github.caioeduardopereirafelix.financeapi.service.validator;

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

    public void validate(User user) {
        if (existUser(user)){
            throw new RegistrationDuplicated("Email already registered for another user");
        }
    }


    private boolean existUser(User user){

        Optional<User> userFound =
                repository.findByEmail(user.getEmail());

        if (userFound.isEmpty()){
            return false;
        }

        if (user.getId() == null){
            return true;
        }

        return !user.getId().equals(userFound.get().getId());
    }
}

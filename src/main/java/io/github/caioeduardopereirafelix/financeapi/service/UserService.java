package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.model.dto.UserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.UserMapper;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import io.github.caioeduardopereirafelix.financeapi.service.validator.UserValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;
    private final UserValidator userValidator;


    public User createUser(@Valid @RequestBody UserDTO dto){
        //utilizar senha terminal, adicionado security para proteger endpoint
        var userMap = mapper.toUser(dto);
        userValidator.validatePassword(dto.password());
        userValidator.validateEmail(dto.email());
        userValidator.validateName(dto.name());
        userMap.setPassword(encoder.encode(dto.password()));
        return repository.save(userMap);
    }

    public Optional<User> findById(UUID id){
        return repository.findById(id);
    }
}

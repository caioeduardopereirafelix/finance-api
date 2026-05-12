package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.model.dto.user.CreateUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.UpdateUserDTO;
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


    public User createUser(@Valid @RequestBody CreateUserDTO dto){

        var userMap = mapper.toUser(dto);

        userMap.setPassword(encoder.encode(dto.password()));

        userValidator.validate(userMap);

        return repository.save(userMap);
    }

    public void save(User user){
        repository.save(user);
    }

    public Optional<User> findById(UUID id){
        return repository.findById(id);
    }

    public void deleteUser(User user) {
        repository.delete(user);
    }

    public User updateUser(UUID id, UpdateUserDTO request) {
        var user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(request.name());
        user.setEmail(request.email());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(encoder.encode(request.password()));
        }

        userValidator.validate(user);

        return repository.save(user);
    }
}

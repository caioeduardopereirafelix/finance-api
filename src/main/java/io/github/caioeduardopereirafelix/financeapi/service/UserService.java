package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.EmailInvalidException;
import io.github.caioeduardopereirafelix.financeapi.model.dto.UserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.UsuarioMapper;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository repository;
    private final UsuarioMapper mapper;
    private final PasswordEncoder encoder;


    public User createUser(@Valid @RequestBody UserDTO dto){
        //utilizar senha terminal, adicionado security para proteger endpoint
        var userMap = mapper.toUser(dto);
        userMap.setPassword(encoder.encode(dto.password()));
        if (repository.existsByEmail(dto.email())) throw new EmailInvalidException("Email already exists");
        return repository.save(userMap);
    }
}

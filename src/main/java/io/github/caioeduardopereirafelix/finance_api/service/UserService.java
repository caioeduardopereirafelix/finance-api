package io.github.caioeduardopereirafelix.finance_api.service;

import io.github.caioeduardopereirafelix.finance_api.model.dto.UserDTO;
import io.github.caioeduardopereirafelix.finance_api.model.entity.User;
import io.github.caioeduardopereirafelix.finance_api.model.mapper.UsuarioMapper;
import io.github.caioeduardopereirafelix.finance_api.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final UsuarioMapper mapper;
    private final PasswordEncoder encoder;

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserDTO dto){
        //utilizar senha terminal, adicionado security para proteger endpoint
        var userMap = mapper.toUser(dto);
        userMap.setPassword(encoder.encode(dto.password()));

        repository.save(userMap);
        return ResponseEntity.ok(userMap);
    }
}

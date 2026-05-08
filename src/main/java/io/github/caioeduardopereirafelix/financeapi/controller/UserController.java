package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.model.dto.UserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.UserMapper;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import io.github.caioeduardopereirafelix.financeapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserDTO dto) {

        userService.createUser(dto);
        return new ResponseEntity(dto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity getDetails(@PathVariable("id") String id){
        var idUser = UUID.fromString(id);
        Optional<User> userOptional = userService.findById(idUser);

        if (userOptional.isPresent()){
            var userPresent = userOptional.get();
            UserDTO userSearch = mapper.toUserDto(userPresent);
            return ResponseEntity.ok(userSearch);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity deleteUser(@PathVariable("id") String id){

        var idUser = UUID.fromString(id);

        Optional<User> optionalUser = userService.findById(idUser);

        if (optionalUser.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(optionalUser.get());
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity updateUser(@PathVariable("id")String id, @RequestBody UserDTO userDTO){
        var idUser = UUID.fromString(id);

        Optional<User> optional = userService.findById(idUser);

        if (optional.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        var user = optional.get();
        user.setEmail(userDTO.email());
        user.setName(userDTO.name());
        if (userDTO.password() != null && !userDTO.password().isBlank()){

        user.setPassword(encoder.encode(userDTO.password()));
        userService.save(user);
        }
        return ResponseEntity.accepted().build();
    }
}

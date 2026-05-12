package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.model.dto.user.CreateUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.ResponseUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.UpdateUserDTO;
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
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserDTO dto) {

        var user = userService.createUser(dto);

        ResponseUserDTO responseUserDTO = mapper.toUserResponse(user);

        return new ResponseEntity(responseUserDTO, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity getDetails(@PathVariable("id") String id){
        var idUser = UUID.fromString(id);
        Optional<User> userOptional = userService.findById(idUser);

        if (userOptional.isPresent()){
            var userPresent = userOptional.get();
            var response = mapper.toUserResponse(userPresent);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity deleteUser(@PathVariable("id") String id){

        var idUser = UUID.fromString(id);

        Optional<User> optionalUser = userService.findById(idUser);

        if (optionalUser.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        userService.deleteUser(optionalUser.get());
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/{userId}")
    public ResponseEntity updateUser(
            @PathVariable("id")String id,
            @RequestBody UpdateUserDTO updateUserDTO){

        var idUser = UUID.fromString(id);

        User userUpdate = userService.updateUser(idUser, updateUserDTO);

        return ResponseEntity.ok(mapper.toUserResponse(userUpdate));
    }
}

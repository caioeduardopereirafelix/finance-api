package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.model.dto.user.CreateUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.ResponseUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.UpdateUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.UserMapper;
import io.github.caioeduardopereirafelix.financeapi.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper mapper;

    @PostMapping
    public ResponseEntity<ResponseUserDTO> createUser(@Valid @RequestBody CreateUserDTO dto) {

        var user = userService.createUser(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toUserResponse(user));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ResponseUserDTO> getDetails(@PathVariable("userId") UUID userId){

        return userService.findById(userId)
                .map(mapper::toUserResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable("userId") UUID userId){

        userService.deleteById(userId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ResponseUserDTO> updateUser(
            @PathVariable("userId") UUID userId,
            @RequestBody UpdateUserDTO updateUserDTO){

        User userUpdate = userService.updateUser(userId, updateUserDTO);

        return ResponseEntity.ok(mapper.toUserResponse(userUpdate));
    }
}

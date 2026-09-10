package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.LoginDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.RefreshTokenRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.RequestAuthDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.ResponseAuthDTO;
import io.github.caioeduardopereirafelix.financeapi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody @Valid RequestAuthDTO authDTO) {
        authService.registerUser(authDTO);
    }

    @PostMapping("/login")
    public ResponseAuthDTO login(@RequestBody @Valid LoginDTO login){
        return authService.login(login);
    }

    @PostMapping("/refresh")
    public ResponseAuthDTO refresh(@RequestBody @Valid RefreshTokenRequestDTO request){
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody @Valid RefreshTokenRequestDTO request){
        authService.logout(request);
    }

    @GetMapping("/ping")
    public String ping(){
        return "API RODANDO";
    }
}

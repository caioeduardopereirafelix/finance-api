package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.ForgotPasswordRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.LoginDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.RefreshTokenRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.RequestAuthDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.ResetPasswordRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.ResponseAuthDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.VerifyEmailRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.service.AuthService;
import io.github.caioeduardopereirafelix.financeapi.service.EmailVerificationService;
import io.github.caioeduardopereirafelix.financeapi.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

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

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@RequestBody @Valid ForgotPasswordRequestDTO request){
        passwordResetService.requestReset(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@RequestBody @Valid ResetPasswordRequestDTO request){
        passwordResetService.resetPassword(request.token(), request.password());
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyEmail(@RequestBody @Valid VerifyEmailRequestDTO request){
        emailVerificationService.verify(request.token());
    }

    @GetMapping("/ping")
    public String ping(){
        return "API RODANDO";
    }
}

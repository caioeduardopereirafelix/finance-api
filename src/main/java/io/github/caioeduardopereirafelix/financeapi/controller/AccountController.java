package io.github.caioeduardopereirafelix.financeapi.controller;

import io.github.caioeduardopereirafelix.financeapi.config.SecurityUtils;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.AccountResponseDTO;
import io.github.caioeduardopereirafelix.financeapi.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final SecurityUtils securityUtils;
    private final EmailVerificationService emailVerificationService;

    @GetMapping
    public AccountResponseDTO me() {
        var user = securityUtils.getAuthenticatedUser();
        return new AccountResponseDTO(user.getName(), user.getEmail(), user.getEmailVerifiedAt() != null);
    }

    @PostMapping("/email-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendVerification() {
        emailVerificationService.resend(securityUtils.getAuthenticatedUser());
    }
}

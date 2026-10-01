package io.github.caioeduardopereirafelix.financeapi.model.dto.auth;

import io.github.caioeduardopereirafelix.financeapi.config.EmailPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequestDTO(
        @NotBlank(message = "email it cannot  be empty")
        @Email(message = "invalid email format")
        String email) {

    public ForgotPasswordRequestDTO {
        email = EmailPolicy.normalize(email);
    }
}

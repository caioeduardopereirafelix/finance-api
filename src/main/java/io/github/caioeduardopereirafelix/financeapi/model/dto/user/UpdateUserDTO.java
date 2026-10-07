package io.github.caioeduardopereirafelix.financeapi.model.dto.user;

import io.github.caioeduardopereirafelix.financeapi.config.EmailPolicy;
import io.github.caioeduardopereirafelix.financeapi.config.NamePolicy;
import io.github.caioeduardopereirafelix.financeapi.config.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserDTO(
        @Size(max = NamePolicy.MAX_LENGTH, message = "O nome deve ter no máximo {max} caracteres")
        @Pattern(regexp = ".*\\S.*", message = "O nome não pode ficar vazio")
        String name,

        @Email(message = "E-mail inválido", regexp = EmailPolicy.PATTERN)
        String email,

        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
                message = "A senha deve ter entre {min} e {max} caracteres")
        String password,

        @Size(max = PasswordPolicy.MAX_LENGTH, message = "A senha atual é longa demais")
        String currentPassword) {

    public UpdateUserDTO {
        email = EmailPolicy.normalize(email);
    }
}

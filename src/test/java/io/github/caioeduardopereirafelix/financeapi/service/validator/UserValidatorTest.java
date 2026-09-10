package io.github.caioeduardopereirafelix.financeapi.service.validator;

import io.github.caioeduardopereirafelix.financeapi.exceptions.RegistrationDuplicated;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserValidator validator;

    @Test
    void naoDeveFalharQuandoUsuarioExistenteTrocaParaEmailAindaNaoCadastrado() {
        var user = User.builder().id(UUID.randomUUID()).email("novo@test.com").build();

        when(repository.findByEmail("novo@test.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.validate(user));
    }

    @Test
    void naoDeveFalharQuandoUsuarioMantemOProprioEmail() {
        var id = UUID.randomUUID();
        var user = User.builder().id(id).email("caio@test.com").build();

        when(repository.findByEmail("caio@test.com")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> validator.validate(user));
    }

    @Test
    void deveLancarExcecaoQuandoEmailPertenceAOutroUsuario() {
        var outro = User.builder().id(UUID.randomUUID()).email("caio@test.com").build();
        var user = User.builder().id(UUID.randomUUID()).email("caio@test.com").build();

        when(repository.findByEmail("caio@test.com")).thenReturn(Optional.of(outro));

        assertThrows(RegistrationDuplicated.class, () -> validator.validate(user));
    }

    @Test
    void deveLancarExcecaoQuandoNovoUsuarioUsaEmailJaCadastrado() {
        var existente = User.builder().id(UUID.randomUUID()).email("caio@test.com").build();
        var novo = User.builder().email("caio@test.com").build();

        when(repository.findByEmail("caio@test.com")).thenReturn(Optional.of(existente));

        assertThrows(RegistrationDuplicated.class, () -> validator.validate(novo));
    }
}

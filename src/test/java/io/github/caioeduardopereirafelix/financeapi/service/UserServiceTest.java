package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.config.SecurityUtils;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.UpdateUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.RolesUser;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.RolesTypeEnum;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.UserMapper;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import io.github.caioeduardopereirafelix.financeapi.service.validator.UserValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;
    @Mock
    private UserMapper mapper;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private UserValidator userValidator;
    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private UserService userService;

    private User userWith(UUID id, RolesTypeEnum role) {
        return User.builder()
                .id(id)
                .email("user@test.com")
                .roles(List.of(RolesUser.builder().name(role.name()).build()))
                .build();
    }

    private UpdateUserDTO anyUpdate() {
        return new UpdateUserDTO("Nome", "novo@test.com", null);
    }

    @Test
    void usuarioComumNaoPodeLerCadastroDeOutroUsuario() {
        var autenticado = userWith(UUID.randomUUID(), RolesTypeEnum.ROLE_USER);
        when(securityUtils.getAuthenticatedUser()).thenReturn(autenticado);

        assertThrows(AccessDeniedException.class, () -> userService.findById(UUID.randomUUID()));

        verify(repository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usuarioComumNaoPodeAtualizarOutroUsuario() {
        var autenticado = userWith(UUID.randomUUID(), RolesTypeEnum.ROLE_USER);
        when(securityUtils.getAuthenticatedUser()).thenReturn(autenticado);

        var alvo = UUID.randomUUID();
        var update = anyUpdate();

        assertThrows(AccessDeniedException.class, () -> userService.updateUser(alvo, update));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usuarioComumNaoPodeApagarOutroUsuario() {
        var autenticado = userWith(UUID.randomUUID(), RolesTypeEnum.ROLE_USER);
        when(securityUtils.getAuthenticatedUser()).thenReturn(autenticado);

        var alvo = UUID.randomUUID();

        assertThrows(AccessDeniedException.class, () -> userService.deleteById(alvo));

        verify(repository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void usuarioComumPodeLerOProprioCadastro() {
        var id = UUID.randomUUID();
        var autenticado = userWith(id, RolesTypeEnum.ROLE_USER);
        when(securityUtils.getAuthenticatedUser()).thenReturn(autenticado);
        when(repository.findById(id)).thenReturn(Optional.of(autenticado));

        assertEquals(Optional.of(autenticado), userService.findById(id));
    }

    @Test
    void adminPodeLerCadastroDeQualquerUsuario() {
        var admin = userWith(UUID.randomUUID(), RolesTypeEnum.ROLE_ADMIN);
        var outro = UUID.randomUUID();
        when(securityUtils.getAuthenticatedUser()).thenReturn(admin);
        when(repository.findById(outro)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), userService.findById(outro));

        verify(repository).findById(outro);
    }
}

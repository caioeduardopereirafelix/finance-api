package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.config.SecurityUtils;
import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
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
    @Mock
    private io.github.caioeduardopereirafelix.financeapi.bank.BankConnectionService bankConnections;
    @Mock
    private EmailVerificationService emailVerificationService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AccountNotifications accountNotifications;

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
        return new UpdateUserDTO("Nome", "novo@test.com", null, null);
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
    void apagarAPropriaContaRevogaAsConexoesBancariasAntesDeApagar() {
        var id = UUID.randomUUID();
        var autenticado = userWith(id, RolesTypeEnum.ROLE_USER);
        when(securityUtils.getAuthenticatedUser()).thenReturn(autenticado);
        when(repository.findById(id)).thenReturn(Optional.of(autenticado));

        userService.deleteById(id);

        var ordem = org.mockito.Mockito.inOrder(bankConnections, repository);
        ordem.verify(bankConnections).revokeAll(autenticado);
        ordem.verify(repository).delete(autenticado);
    }

    @Test
    void seARevogacaoFalharAContaNaoEApagada() {
        var id = UUID.randomUUID();
        var autenticado = userWith(id, RolesTypeEnum.ROLE_USER);
        when(securityUtils.getAuthenticatedUser()).thenReturn(autenticado);
        when(repository.findById(id)).thenReturn(Optional.of(autenticado));
        org.mockito.Mockito.doThrow(new io.github.caioeduardopereirafelix.financeapi.bank.BankIntegrationException(
                org.springframework.http.HttpStatus.BAD_GATEWAY, "Pluggy fora do ar"))
                .when(bankConnections).revokeAll(autenticado);

        assertThrows(io.github.caioeduardopereirafelix.financeapi.bank.BankIntegrationException.class,
                () -> userService.deleteById(id));

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

    private User ownerWithPassword(UUID id) {
        var user = userWith(id, RolesTypeEnum.ROLE_USER);
        user.setName("Caio");
        user.setPassword("hash-antigo");
        return user;
    }

    @Test
    void trocarAPropriaSenhaExigeASenhaAtual() {
        var id = UUID.randomUUID();
        var dono = ownerWithPassword(id);
        when(securityUtils.getAuthenticatedUser()).thenReturn(dono);
        when(repository.findById(id)).thenReturn(Optional.of(dono));

        var semAtual = new UpdateUserDTO(null, null, "senha-nova-123", null);

        var erro = assertThrows(InvalidFieldException.class, () -> userService.updateUser(id, semAtual));

        assertEquals("currentPassword", erro.getCampo());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void senhaAtualErradaEhRecusadaSemAlterarNada() {
        var id = UUID.randomUUID();
        var dono = ownerWithPassword(id);
        when(securityUtils.getAuthenticatedUser()).thenReturn(dono);
        when(repository.findById(id)).thenReturn(Optional.of(dono));
        when(encoder.matches("errada-123", "hash-antigo")).thenReturn(false);

        var errada = new UpdateUserDTO(null, null, "senha-nova-123", "errada-123");

        assertThrows(InvalidFieldException.class, () -> userService.updateUser(id, errada));

        assertEquals("hash-antigo", dono.getPassword());
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(refreshTokenService, never()).revokeAllFor(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void trocarASenhaRevogaAsSessoesEAvisaPorEmail() {
        var id = UUID.randomUUID();
        var dono = ownerWithPassword(id);
        when(securityUtils.getAuthenticatedUser()).thenReturn(dono);
        when(repository.findById(id)).thenReturn(Optional.of(dono));
        when(encoder.matches("atual-123", "hash-antigo")).thenReturn(true);
        when(encoder.encode("senha-nova-123")).thenReturn("hash-novo");
        when(repository.save(dono)).thenReturn(dono);

        userService.updateUser(id, new UpdateUserDTO(null, null, "senha-nova-123", "atual-123"));

        assertEquals("hash-novo", dono.getPassword());
        verify(refreshTokenService).revokeAllFor(dono);
        verify(accountNotifications).passwordChanged(dono);
    }

    @Test
    void adminTrocaASenhaDeOutroUsuarioSemSaberASenhaAtualMasRevogaAsSessoes() {
        var admin = userWith(UUID.randomUUID(), RolesTypeEnum.ROLE_ADMIN);
        var alvoId = UUID.randomUUID();
        var alvo = ownerWithPassword(alvoId);
        when(securityUtils.getAuthenticatedUser()).thenReturn(admin);
        when(repository.findById(alvoId)).thenReturn(Optional.of(alvo));
        when(encoder.encode("senha-nova-123")).thenReturn("hash-novo");
        when(repository.save(alvo)).thenReturn(alvo);

        userService.updateUser(alvoId, new UpdateUserDTO(null, null, "senha-nova-123", null));

        assertEquals("hash-novo", alvo.getPassword());
        verify(refreshTokenService).revokeAllFor(alvo);
        verify(accountNotifications).passwordChanged(alvo);
    }

    @Test
    void atualizarSoONomeNaoMexeNoEmailNemNaSenhaENaoRevogaNada() {
        var id = UUID.randomUUID();
        var dono = ownerWithPassword(id);
        when(securityUtils.getAuthenticatedUser()).thenReturn(dono);
        when(repository.findById(id)).thenReturn(Optional.of(dono));
        when(repository.save(dono)).thenReturn(dono);

        userService.updateUser(id, new UpdateUserDTO("Novo Nome", null, null, null));

        assertEquals("Novo Nome", dono.getName());
        assertEquals("user@test.com", dono.getEmail());
        assertEquals("hash-antigo", dono.getPassword());
        verify(refreshTokenService, never()).revokeAllFor(org.mockito.ArgumentMatchers.any());
        verify(emailVerificationService, never()).sendInitial(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void trocarOEmailDesfazAConfirmacaoEMandaOutroLink() {
        var id = UUID.randomUUID();
        var dono = ownerWithPassword(id);
        dono.setEmailVerifiedAt(java.time.Instant.now());
        when(securityUtils.getAuthenticatedUser()).thenReturn(dono);
        when(repository.findById(id)).thenReturn(Optional.of(dono));
        when(repository.save(dono)).thenReturn(dono);

        userService.updateUser(id, new UpdateUserDTO(null, "novo@test.com", null, null));

        assertEquals("novo@test.com", dono.getEmail());
        assertEquals(null, dono.getEmailVerifiedAt());
        verify(emailVerificationService).sendInitial(dono);
    }
}

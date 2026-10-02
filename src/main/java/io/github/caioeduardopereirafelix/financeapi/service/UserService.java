package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.bank.BankConnectionService;
import io.github.caioeduardopereirafelix.financeapi.config.SecurityUtils;
import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidFieldException;
import io.github.caioeduardopereirafelix.financeapi.exceptions.UserNotFound;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.CreateUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.user.UpdateUserDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.RolesTypeEnum;
import io.github.caioeduardopereirafelix.financeapi.model.mapper.UserMapper;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import io.github.caioeduardopereirafelix.financeapi.service.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;
    private final UserValidator userValidator;
    private final SecurityUtils securityUtils;
    private final BankConnectionService bankConnections;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;
    private final AccountNotifications accountNotifications;

    public User createUser(CreateUserDTO dto){

        var userMap = mapper.toUser(dto);

        userMap.setPassword(encoder.encode(dto.password()));

        userValidator.validate(userMap);

        userMap.setEmailVerifiedAt(Instant.now());

        return repository.save(userMap);
    }

    public Optional<User> findById(UUID id){

        checkAccessTo(id);

        return repository.findById(id);
    }

    public void deleteById(UUID id) {

        checkAccessTo(id);

        var user = repository.findById(id)
                .orElseThrow(() -> new UserNotFound("User not found"));

        bankConnections.revokeAll(user);

        repository.delete(user);
    }

    public User updateUser(UUID id, UpdateUserDTO request) {

        checkAccessTo(id);

        var user = repository.findById(id)
                .orElseThrow(() -> new UserNotFound("User not found"));

        boolean emailChanged = request.email() != null && !request.email().equals(user.getEmail());
        boolean passwordChanged = request.password() != null && !request.password().isBlank();

        if (passwordChanged && securityUtils.getAuthenticatedUser().getId().equals(id)) {
            requireCurrentPassword(user, request.currentPassword());
        }

        if (request.name() != null) {
            user.setName(request.name());
        }

        if (emailChanged) {
            user.setEmail(request.email());
            user.setEmailVerifiedAt(null);
        }

        if (passwordChanged) {
            user.setPassword(encoder.encode(request.password()));
        }

        userValidator.validate(user);

        User saved = repository.save(user);

        if (emailChanged) {
            emailVerificationService.sendInitial(saved);
        }

        if (passwordChanged) {
            refreshTokenService.revokeAllFor(saved);
            accountNotifications.passwordChanged(saved);
        }

        return saved;
    }

    private void requireCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new InvalidFieldException("currentPassword", "Informe a senha atual para trocar a senha");
        }
        if (!encoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidFieldException("currentPassword", "Senha atual incorreta");
        }
    }

    private void checkAccessTo(UUID targetUserId) {

        User authenticated = securityUtils.getAuthenticatedUser();

        boolean isAdmin = authenticated.getAuthorities().stream()
                .anyMatch(authority -> RolesTypeEnum.ROLE_ADMIN.name().equals(authority.getAuthority()));

        if (!isAdmin && !authenticated.getId().equals(targetUserId)) {
            throw new AccessDeniedException("Access denied to another user's data");
        }
    }
}

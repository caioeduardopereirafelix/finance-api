package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.config.SecurityUtils;
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

    public User createUser(CreateUserDTO dto){

        var userMap = mapper.toUser(dto);

        userMap.setPassword(encoder.encode(dto.password()));

        userValidator.validate(userMap);

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

        repository.delete(user);
    }

    public User updateUser(UUID id, UpdateUserDTO request) {

        checkAccessTo(id);

        var user = repository.findById(id)
                .orElseThrow(() -> new UserNotFound("User not found"));

        user.setName(request.name());
        user.setEmail(request.email());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(encoder.encode(request.password()));
        }

        userValidator.validate(user);

        return repository.save(user);
    }

    /**
     * Um usuario comum so enxerga o proprio cadastro; ADMIN enxerga qualquer um.
     * A checagem vem antes da busca no banco de proposito: assim um id de outro
     * usuario responde sempre 403, sem revelar se ele existe ou nao.
     */
    private void checkAccessTo(UUID targetUserId) {

        User authenticated = securityUtils.getAuthenticatedUser();

        boolean isAdmin = authenticated.getAuthorities().stream()
                .anyMatch(authority -> RolesTypeEnum.ROLE_ADMIN.name().equals(authority.getAuthority()));

        if (!isAdmin && !authenticated.getId().equals(targetUserId)) {
            throw new AccessDeniedException("Access denied to another user's data");
        }
    }
}

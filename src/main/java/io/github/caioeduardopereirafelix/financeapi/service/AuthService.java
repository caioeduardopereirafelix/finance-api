package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.config.TokenProvider;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.LoginDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.RefreshTokenRequestDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.RequestAuthDTO;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.ResponseAuthDTO;
import io.github.caioeduardopereirafelix.financeapi.model.entity.RolesUser;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.RolesTypeEnum;
import io.github.caioeduardopereirafelix.financeapi.repository.RolesUserRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RolesUserRepository rolesUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttempts;
    private final EmailVerificationService emailVerificationService;
    private final AccountNotifications accountNotifications;

    @Value("${api.security.token.expiration}")
    private long expirationTime;

    public void registerUser(RequestAuthDTO requestAuthDTO){

        String encodedPassword = passwordEncoder.encode(requestAuthDTO.password());

        var existing = userRepository.findByEmail(requestAuthDTO.email());
        if (existing.isPresent()) {
            accountNotifications.registrationAttemptOnExistingEmail(existing.get());
            return;
        }

        var role = rolesUserRepository.findByName(RolesTypeEnum.ROLE_USER.name())
                .orElseGet(() -> rolesUserRepository.save(RolesUser.builder()
                        .name(RolesTypeEnum.ROLE_USER.name()).build()));

        var saved = userRepository.save(User.builder()
                .name(requestAuthDTO.user())
                .email(requestAuthDTO.email())
                .roles(List.of(role))
                .password(encodedPassword)
                .build());

        emailVerificationService.sendInitial(saved);
    }


    public ResponseAuthDTO login(LoginDTO login){

        loginAttempts.checkAllowed(login.email());

        Authentication authentication;
        try {
            authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(login.email(), login.password()));
        } catch (BadCredentialsException e) {
            loginAttempts.recordFailure(login.email());
            throw e;
        }
        loginAttempts.recordSuccess(login.email());

        var user = (User) authentication.getPrincipal();

        return new ResponseAuthDTO(
                tokenProvider.generateToken(authentication),
                expirationTime,
                refreshTokenService.generate(user));
    }


    public ResponseAuthDTO refresh(RefreshTokenRequestDTO request){

        User user = refreshTokenService.consume(request.refreshToken());

        return new ResponseAuthDTO(
                tokenProvider.generateToken(user),
                expirationTime,
                refreshTokenService.generate(user));
    }

    public void logout(RefreshTokenRequestDTO request){
        refreshTokenService.revoke(request.refreshToken());
    }
}

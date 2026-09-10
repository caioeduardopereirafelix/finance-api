package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.config.TokenProvider;
import io.github.caioeduardopereirafelix.financeapi.exceptions.EmailAlreadyExistException;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    @Value("${api.security.token.expiration}")
    private long expirationTime;

    public void registerUser(RequestAuthDTO requestAuthDTO){

        if (userRepository.findByEmail(requestAuthDTO.email()).isPresent()){
            throw new EmailAlreadyExistException("Email already registered");
        }

        var role = rolesUserRepository.findByName(RolesTypeEnum.ROLE_USER.name())
                .orElseGet(() -> rolesUserRepository.save(RolesUser.builder()
                        .name(RolesTypeEnum.ROLE_USER.name()).build()));

        userRepository.save(User.builder()
                .name(requestAuthDTO.user())
                .email(requestAuthDTO.email())
                .roles(List.of(role))
                .password(passwordEncoder.encode(requestAuthDTO.password()))
                .build());
    }


    public ResponseAuthDTO login(LoginDTO login){

        var authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(login.email(), login.password()));

        var user = (User) authentication.getPrincipal();

        return new ResponseAuthDTO(
                tokenProvider.generateToken(authentication),
                expirationTime,
                refreshTokenService.generate(user));
    }

    /**
     * Troca um refresh token valido por um novo par de tokens. O refresh token
     * apresentado e invalidado no processo.
     */
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

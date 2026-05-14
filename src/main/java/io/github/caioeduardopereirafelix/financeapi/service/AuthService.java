package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.config.TokenProvider;
import io.github.caioeduardopereirafelix.financeapi.model.dto.auth.LoginDTO;
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
    @Value("${api.security.token.expiration}")
    private long expirationTime;

    public void registerUser(RequestAuthDTO requestAuthDTO) throws RuntimeException{
        var user = userRepository.findByEmail(requestAuthDTO.email())
                .orElse(null);

        if (user != null){
            throw new RuntimeException("User already register with email ");
        }

        var role = rolesUserRepository.findByName(RolesTypeEnum.ROLE_USER.name())
                .orElseGet(() -> rolesUserRepository.save(RolesUser.builder()
                        .name(RolesTypeEnum.ROLE_USER.name()).build()));

        userRepository.save(User.builder()
                .name(requestAuthDTO.user())
                .email(requestAuthDTO.email())
                .roles(List.of(role))
                .password(passwordEncoder.encode(requestAuthDTO.senha()))
                .build());
    }


    public ResponseAuthDTO login(LoginDTO login) throws Exception{
        //try{

            var autentication =
                    authenticationManager
                            .authenticate(new UsernamePasswordAuthenticationToken(login.email(), login.senha()));
            var token = tokenProvider.generateToken(autentication);

            return new ResponseAuthDTO(token, expirationTime);

            //authentication provider -> userdetailsservice -> passwordEncoder.matches() -> authenticated
        //}catch (BadCredentialsException e){
        //    throw new BadCredentialsException("Credentials invalid");
       // } catch (Exception e) {
        //    throw new RuntimeException(e);
      //  }
    }
}

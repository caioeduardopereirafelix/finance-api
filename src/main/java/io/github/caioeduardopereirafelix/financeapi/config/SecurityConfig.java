package io.github.caioeduardopereirafelix.financeapi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity

public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        exception -> exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                    response.setStatus(HttpStatus.FORBIDDEN.value());
                                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,"/v1/auth","/v1/auth/**")
                        .permitAll()

                        .requestMatchers(HttpMethod.GET, "/v1/auth/**")
                        .permitAll()

                        // Pagina de erro do Spring. Sem isso, um erro interno (500) era redirecionado para
                        // /error, que exigia login, e chegava ao cliente como um 401 enganoso.
                        .requestMatchers("/error")
                        .permitAll()

                        // Webhook da Pluggy: sem login; a autenticidade e o segredo no caminho.
                        .requestMatchers(HttpMethod.POST, "/webhooks/pluggy/**")
                        .permitAll()

                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()

                        // O actuator nao e mais publicado na porta da API: ele roda
                        // numa porta separada (management.server.port), que nao deve
                        // ser exposta fora da rede interna.
                        .requestMatchers("/actuator/**")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST,"/user","/user/**")
                        .hasRole("ADMIN")

                        // GET/PUT/DELETE de usuario exigem autenticacao aqui e, no
                        // UserService, que o solicitante seja o dono do cadastro ou ADMIN.
                        .requestMatchers(HttpMethod.GET, "/user", "/user/**")
                        .hasAnyRole("ADMIN", "USER")

                        .requestMatchers(HttpMethod.PUT, "/user", "/user/**")
                        .hasAnyRole("ADMIN", "USER")

                        .requestMatchers(HttpMethod.DELETE, "/user", "/user/**")
                        .hasAnyRole("ADMIN", "USER")

                        .requestMatchers(HttpMethod.GET, "/transaction","/transaction/**")
                        .hasAnyRole("ADMIN", "USER")

                        .requestMatchers(HttpMethod.DELETE, "/transaction", "/transaction/**")
                        .hasAnyRole("ADMIN", "USER")

                        .requestMatchers(HttpMethod.POST, "/transaction", "/transaction/**")
                        .hasAnyRole("ADMIN", "USER")

                        .requestMatchers(HttpMethod.PUT, "/transaction", "/transaction/**")
                        .hasAnyRole("ADMIN", "USER")

                        .requestMatchers(HttpMethod.PATCH, "/transaction/**")
                        .hasAnyRole("ADMIN", "USER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder encoder(){
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}

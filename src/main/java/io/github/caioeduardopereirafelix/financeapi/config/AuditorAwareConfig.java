package io.github.caioeduardopereirafelix.financeapi.config;

import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class AuditorAwareConfig {

    /**
     * Sem este bean as anotacoes @CreatedBy/@LastModifiedBy da AuditingClass
     * nunca eram preenchidas, mesmo com @EnableJpaAuditing ligado.
     *
     * Retorna vazio quando nao ha usuario autenticado (cadastro publico,
     * por exemplo), e nesse caso as colunas ficam nulas.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(User.class::isInstance)
                .map(User.class::cast)
                .map(User::getId)
                .map(String::valueOf);
    }
}

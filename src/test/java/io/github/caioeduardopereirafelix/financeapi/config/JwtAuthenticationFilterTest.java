package io.github.caioeduardopereirafelix.financeapi.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenProvider tokenProvider;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private FilterChain chain;

    @AfterEach
    void limpa() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tokenValidoDeUsuarioApagadoSegueSemAutenticarEmVezDeEstourar() throws Exception {
        var request = new MockHttpServletRequest("GET", "/transaction");
        request.addHeader("Authorization", "Bearer token-valido");
        var response = new MockHttpServletResponse();
        when(tokenProvider.isTokenValid("token-valido")).thenReturn(true);
        when(tokenProvider.getUserName("token-valido")).thenReturn("apagado@test.com");
        when(userDetailsService.loadUserByUsername("apagado@test.com"))
                .thenThrow(new UsernameNotFoundException("apagado@test.com"));

        new JwtAuthenticationFilter(tokenProvider, userDetailsService).doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}

package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "api.security.rate-limit.enabled=true",
        "api.security.rate-limit.trust-forwarded-for=true",
        "api.security.rate-limit.register-per-hour=3",
        "api.security.rate-limit.forgot-per-hour=2",
        "api.security.rate-limit.login-per-minute=4"
})
class RateLimitIntegrationTest extends ApiIntegrationTestSupport {

    private int register(String ip) throws Exception {
        return mockMvc.perform(post("/v1/auth/register")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"rl-%s@test.com","user":"Teste","password":"senha-segura-1"}
                                """.formatted(UUID.randomUUID())))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void cadastrosAlemDoLimiteDoMesmoEnderecoRecebem429ComRetryAfter() throws Exception {
        String ip = "198.51.100." + (int) (Math.random() * 200 + 10);

        for (int i = 0; i < 3; i++) {
            assertEquals(201, register(ip));
        }

        mockMvc.perform(post("/v1/auth/register")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"x-%s@test.com","user":"Teste","password":"senha-segura-1"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void outroEnderecoNaoEAfetado() throws Exception {
        String ipA = "203.0.113." + (int) (Math.random() * 100 + 10);
        String ipB = "203.0.113." + (int) (Math.random() * 100 + 120);
        for (int i = 0; i < 3; i++) {
            register(ipA);
        }

        assertEquals(429, register(ipA));
        assertEquals(201, register(ipB));
    }

    @Test
    void recuperarSenhaTambemTemLimitePorEndereco() throws Exception {
        String ip = "192.0.2." + (int) (Math.random() * 200 + 10);
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/v1/auth/forgot-password")
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"qualquer@test.com\"}"))
                    .andExpect(status().isAccepted());
        }

        mockMvc.perform(post("/v1/auth/forgot-password")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"qualquer@test.com\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void loginTambemTemLimitePorEnderecoMesmoComSenhaCerta() throws Exception {
        var account = registerAndLogin();
        String ip = "100.64.1." + (int) (Math.random() * 200 + 10);

        int last = 0;
        for (int i = 0; i < 5; i++) {
            last = mockMvc.perform(post("/v1/auth/login")
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"email":"%s","password":"%s"}
                                    """.formatted(account.email(), account.password())))
                    .andReturn().getResponse().getStatus();
        }

        assertEquals(429, last);
    }
}

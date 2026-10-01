package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(PasswordResetIntegrationTest.RecordingMail.class)
class PasswordResetPostgresIntegrationTest extends PostgresIntegrationTestSupport {

    private static final Pattern LINK = Pattern.compile("/redefinir-senha#token=([A-Za-z0-9_-]+)");

    @Autowired
    private JdbcTemplate jdbc;

    private String lastTokenSentTo(String email) {
        var sent = PasswordResetIntegrationTest.RecordingMail.SENT.stream()
                .filter(s -> s.to().equals(email)).toList();
        Matcher matcher = LINK.matcher(sent.get(sent.size() - 1).body());
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private int storedTokens(String email) {
        return jdbc.queryForObject("""
                select count(*) from password_reset_tokens t join users u on u.id = t.user_id
                where u.email = ?
                """, Integer.class, email);
    }

    @Test
    void fluxoCompletoContraOPostgresComTokenDeUsoUnico() throws Exception {
        var account = registerAndLogin();

        mockMvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(account.email())))
                .andExpect(status().isAccepted());
        assertEquals(1, storedTokens(account.email()));
        String token = lastTokenSentTo(account.email());

        mockMvc.perform(post("/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"senha-nova-123"}
                                """.formatted(token)))
                .andExpect(status().isNoContent());
        assertEquals(0, storedTokens(account.email()));

        mockMvc.perform(post("/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"outra-senha-123"}
                                """.formatted(token)))
                .andExpect(status().isBadRequest());
        assertEquals(200, loginStatus(account.email(), "senha-nova-123"));
    }

    @Test
    void apagarAContaApagaOsTokensPendentes() throws Exception {
        var account = registerAndLogin();
        mockMvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(account.email())))
                .andExpect(status().isAccepted());
        String userId = jdbc.queryForObject("select id::text from users where email = ?", String.class, account.email());

        mockMvc.perform(delete("/user/" + userId).header("Authorization", account.bearer()))
                .andExpect(status().is2xxSuccessful());

        assertEquals(0, storedTokens(account.email()));
    }

    private int loginStatus(String email, String password) throws Exception {
        return mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andReturn().getResponse().getStatus();
    }
}

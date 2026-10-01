package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(PasswordResetIntegrationTest.RecordingMail.class)
class EmailVerificationPostgresIntegrationTest extends PostgresIntegrationTestSupport {

    private static final Pattern LINK = Pattern.compile("/confirmar-email#token=([A-Za-z0-9_-]+)");

    @Autowired
    private JdbcTemplate jdbc;

    private int storedTokens(String email) {
        return jdbc.queryForObject("""
                select count(*) from email_verification_tokens t join users u on u.id = t.user_id
                where u.email = ?
                """, Integer.class, email);
    }

    @Test
    void fluxoCompletoContraOPostgres() throws Exception {
        var account = registerAndLoginUnverified();
        assertEquals(1, storedTokens(account.email()));
        assertNull(jdbc.queryForObject("select email_verified_at from users where email = ?",
                java.sql.Timestamp.class, account.email()));

        mockMvc.perform(post("/bank/connect-token").header("Authorization", account.bearer()))
                .andExpect(status().isForbidden());

        var sent = PasswordResetIntegrationTest.RecordingMail.SENT.stream()
                .filter(s -> s.to().equals(account.email())).toList();
        Matcher matcher = LINK.matcher(sent.get(sent.size() - 1).body());
        assertTrue(matcher.find());
        String token = matcher.group(1);

        mockMvc.perform(post("/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(token)))
                .andExpect(status().isNoContent());

        assertEquals(0, storedTokens(account.email()));
        assertNotNull(jdbc.queryForObject("select email_verified_at from users where email = ?",
                java.sql.Timestamp.class, account.email()));
        mockMvc.perform(post("/bank/connect-token").header("Authorization", account.bearer()))
                .andExpect(status().isOk());
    }
}

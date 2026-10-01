package io.github.caioeduardopereirafelix.financeapi.integration;

import io.github.caioeduardopereirafelix.financeapi.repository.EmailVerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(PasswordResetIntegrationTest.RecordingMail.class)
class EmailVerificationIntegrationTest extends ApiIntegrationTestSupport {

    private static final Pattern LINK = Pattern.compile("/confirmar-email#token=([A-Za-z0-9_-]+)");

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    private List<PasswordResetIntegrationTest.Sent> sentTo(String email) {
        return PasswordResetIntegrationTest.RecordingMail.SENT.stream().filter(s -> s.to().equals(email)).toList();
    }

    private String tokenSentTo(String email) {
        var sent = sentTo(email);
        Matcher matcher = LINK.matcher(sent.get(sent.size() - 1).body());
        assertTrue(matcher.find(), "o e-mail deveria trazer o link de confirmacao");
        return matcher.group(1);
    }

    private void verify(String token, int expectedStatus) throws Exception {
        mockMvc.perform(post("/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(token)))
                .andExpect(status().is(expectedStatus));
    }

    private void liberarIntervalo(String email) {
        var userId = userRepository.findByEmail(email).orElseThrow().getId();
        tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .forEach(t -> {
                    t.setCreatedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
                    tokenRepository.save(t);
                });
    }

    @Test
    void cadastroEnviaOLinkEDeixaAContaNaoConfirmada() throws Exception {
        var account = registerAndLoginUnverified();

        assertEquals(1, sentTo(account.email()).size());
        assertEquals("Confirme seu e-mail", sentTo(account.email()).get(0).subject());
        mockMvc.perform(get("/account").header("Authorization", account.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(account.email()))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void semConfirmarOLoginFuncionaMasConectarBancoEBarrado() throws Exception {
        var account = registerAndLoginUnverified();

        mockMvc.perform(get("/transaction").header("Authorization", account.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/bank/connect-token").header("Authorization", account.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Confirme seu e-mail para conectar um banco"));
        mockMvc.perform(post("/bank/connections")
                        .header("Authorization", account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"externalId":"qualquer"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmarLiberaAContaEOLinkSoValeUmaVez() throws Exception {
        var account = registerAndLoginUnverified();
        String token = tokenSentTo(account.email());

        verify(token, 204);

        mockMvc.perform(get("/account").header("Authorization", account.bearer()))
                .andExpect(jsonPath("$.emailVerified").value(true));
        mockMvc.perform(post("/bank/connect-token").header("Authorization", account.bearer()))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
        verify(token, 400);
    }

    @Test
    void tokenInventadoOuExpiradoEhRecusado() throws Exception {
        verify("token-que-nunca-existiu", 400);

        var account = registerAndLoginUnverified();
        String token = tokenSentTo(account.email());
        var userId = userRepository.findByEmail(account.email()).orElseThrow().getId();
        var stored = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId)).findFirst().orElseThrow();
        stored.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        tokenRepository.save(stored);

        verify(token, 400);
    }

    @Test
    void reenvioRespeitaOIntervaloEInvalidaOLinkAnterior() throws Exception {
        var account = registerAndLoginUnverified();
        String first = tokenSentTo(account.email());

        mockMvc.perform(post("/account/email-verification").header("Authorization", account.bearer()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        assertEquals(1, sentTo(account.email()).size());

        liberarIntervalo(account.email());
        mockMvc.perform(post("/account/email-verification").header("Authorization", account.bearer()))
                .andExpect(status().isAccepted());
        assertEquals(2, sentTo(account.email()).size());

        verify(first, 400);
        verify(tokenSentTo(account.email()), 204);
    }

    @Test
    void reenvioParaContaJaConfirmadaNaoMandaNada() throws Exception {
        var account = registerAndLogin();
        int before = sentTo(account.email()).size();

        mockMvc.perform(post("/account/email-verification").header("Authorization", account.bearer()))
                .andExpect(status().isAccepted());

        assertEquals(before, sentTo(account.email()).size());
    }

    @Test
    void reenvioExigeLogin() throws Exception {
        mockMvc.perform(post("/account/email-verification")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/account")).andExpect(status().isUnauthorized());
    }

    @Test
    void trocarOEmailDesfazAConfirmacaoEMandaOutroLink() throws Exception {
        var account = registerAndLogin();
        var userId = userRepository.findByEmail(account.email()).orElseThrow().getId();
        String newEmail = "novo-" + UUID.randomUUID() + "@test.com";

        mockMvc.perform(put("/user/" + userId)
                        .header("Authorization", account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Usuario Teste","email":"%s"}
                                """.formatted(newEmail)))
                .andExpect(status().is2xxSuccessful());

        assertEquals(1, sentTo(newEmail).size());
        assertEquals(null, userRepository.findByEmail(newEmail).orElseThrow().getEmailVerifiedAt());
        verify(tokenSentTo(newEmail), 204);
        assertTrue(userRepository.findByEmail(newEmail).orElseThrow().getEmailVerifiedAt() != null);
    }
}

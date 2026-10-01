package io.github.caioeduardopereirafelix.financeapi.integration;

import io.github.caioeduardopereirafelix.financeapi.mail.EmailSender;
import io.github.caioeduardopereirafelix.financeapi.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(PasswordResetIntegrationTest.RecordingMail.class)
class PasswordResetIntegrationTest extends ApiIntegrationTestSupport {

    record Sent(String to, String subject, String body) {
    }

    @TestConfiguration
    static class RecordingMail {

        static final List<Sent> SENT = new CopyOnWriteArrayList<>();

        @Bean
        @Primary
        EmailSender recordingEmailSender() {
            return (to, subject, body) -> SENT.add(new Sent(to, subject, body));
        }
    }

    private static final Pattern LINK = Pattern.compile("/redefinir-senha#token=([A-Za-z0-9_-]+)");

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    private List<Sent> sentTo(String email) {
        return RecordingMail.SENT.stream().filter(s -> s.to().equals(email) && !s.subject().equals("Confirme seu e-mail")).toList();
    }

    private String tokenSentTo(String email) {
        Matcher matcher = LINK.matcher(sentTo(email).get(sentTo(email).size() - 1).body());
        assertTrue(matcher.find(), "o e-mail deveria trazer o link de redefinicao");
        return matcher.group(1);
    }

    private void forgot(String email, int expectedStatus) throws Exception {
        mockMvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().is(expectedStatus));
    }

    private void reset(String token, String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"%s"}
                                """.formatted(token, password)))
                .andExpect(status().is(expectedStatus));
    }

    private void liberarIntervalo(String email) {
        var user = userRepository.findByEmail(email).orElseThrow();
        tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()))
                .forEach(t -> {
                    t.setCreatedAt(Instant.now().minus(5, ChronoUnit.MINUTES));
                    tokenRepository.save(t);
                });
    }

    @Test
    void emailCadastradoRecebeOLinkEDesconhecidoRecebeAMesmaResposta() throws Exception {
        var account = registerAndLogin();
        String unknown = "ninguem-" + UUID.randomUUID() + "@test.com";

        forgot(account.email(), 202);
        forgot(unknown, 202);

        assertEquals(1, sentTo(account.email()).size());
        assertEquals(0, sentTo(unknown).size());
        assertTrue(sentTo(account.email()).get(0).body().contains("http://localhost:4200/redefinir-senha#token="));
    }

    @Test
    void doisPedidosSeguidosGeramUmSoEmail() throws Exception {
        var account = registerAndLogin();

        forgot(account.email(), 202);
        forgot(account.email(), 202);

        assertEquals(1, sentTo(account.email()).size());
    }

    @Test
    void redefinirTrocaASenhaEInvalidaOLinkEAsSessoes() throws Exception {
        var account = registerAndLogin();
        forgot(account.email(), 202);
        String token = tokenSentTo(account.email());

        reset(token, "senha-nova-123", 204);

        assertEquals(200, mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"senha-nova-123"}
                                """.formatted(account.email())))
                .andReturn().getResponse().getStatus());
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(account.email(), account.password())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(account.refreshToken())))
                .andExpect(status().isUnauthorized());
        reset(token, "outra-senha-123", 400);
        assertEquals("Sua senha foi alterada", sentTo(account.email()).get(1).subject());
    }

    @Test
    void tokenInventadoOuSenhaCurtaSaoRecusados() throws Exception {
        reset("token-que-nunca-existiu", "senha-nova-123", 400);

        var account = registerAndLogin();
        forgot(account.email(), 202);
        reset(tokenSentTo(account.email()), "curta", 422);
    }

    @Test
    void tokenExpiradoEhRecusado() throws Exception {
        var account = registerAndLogin();
        forgot(account.email(), 202);
        String token = tokenSentTo(account.email());
        var userId = userRepository.findByEmail(account.email()).orElseThrow().getId();
        var stored = tokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(userId))
                .findFirst().orElseThrow();
        stored.setExpiresAt(Instant.now().minus(1, ChronoUnit.MINUTES));
        tokenRepository.save(stored);

        reset(token, "senha-nova-123", 400);
    }

    @Test
    void umNovoPedidoInvalidaOLinkAnterior() throws Exception {
        var account = registerAndLogin();
        forgot(account.email(), 202);
        String first = tokenSentTo(account.email());
        liberarIntervalo(account.email());

        forgot(account.email(), 202);
        String second = tokenSentTo(account.email());

        reset(first, "senha-nova-123", 400);
        reset(second, "senha-nova-123", 204);
    }

    @Test
    void redefinirDestravaOLoginBloqueadoPorTentativas() throws Exception {
        var account = registerAndLogin();
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"%s","password":"errada-%d"}
                            """.formatted(account.email(), i)));
        }
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(account.email(), account.password())))
                .andExpect(status().isTooManyRequests());

        forgot(account.email(), 202);
        reset(tokenSentTo(account.email()), "senha-nova-123", 204);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"senha-nova-123"}
                                """.formatted(account.email())))
                .andExpect(status().isOk());
    }
}

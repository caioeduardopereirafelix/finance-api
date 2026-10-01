package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(PasswordResetIntegrationTest.RecordingMail.class)
class EmailNormalizationIntegrationTest extends ApiIntegrationTestSupport {

    private void register(String email, int expectedStatus) throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","user":"Teste","password":"senha-segura-1"}
                                """.formatted(email)))
                .andExpect(status().is(expectedStatus));
    }

    private void login(String email, int expectedStatus) throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"senha-segura-1"}
                                """.formatted(email)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void cadastroGuardaOEmailEmMinusculasELoginAceitaQualquerCaixa() throws Exception {
        String base = "Mixed.Case-" + UUID.randomUUID();

        register("  " + base + "@Test.COM ", 201);

        login(base.toLowerCase() + "@test.com", 200);
        login(base.toUpperCase() + "@TEST.COM", 200);
        assertEquals(base.toLowerCase() + "@test.com",
                userRepository.findByEmail(base.toLowerCase() + "@test.com").orElseThrow().getEmail());
    }

    @Test
    void emailQueSoDifereNaCaixaContaComoRepetido() throws Exception {
        String base = "dup-" + UUID.randomUUID();

        register(base + "@test.com", 201);
        register(base.toUpperCase() + "@Test.com", 409);
    }

    @Test
    void recuperarSenhaDigitandoOutraCaixaEncontraAConta() throws Exception {
        String base = "forgot-" + UUID.randomUUID();
        register(base + "@test.com", 201);

        mockMvc.perform(post("/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(base.toUpperCase() + "@TEST.COM")))
                .andExpect(status().isAccepted());

        long resetMails = PasswordResetIntegrationTest.RecordingMail.SENT.stream()
                .filter(s -> s.to().equals(base + "@test.com") && s.subject().equals("Redefinição de senha"))
                .count();
        assertEquals(1, resetMails);
    }

    @Test
    void cadastroRecusaEmailSemDominioValido() throws Exception {
        register("afafasf@gfsgsg", 422);
        register("sem-arroba.com", 422);
        register("a@b.c", 422);
    }
}

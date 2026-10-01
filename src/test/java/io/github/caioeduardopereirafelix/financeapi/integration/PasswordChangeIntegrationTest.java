package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(PasswordResetIntegrationTest.RecordingMail.class)
class PasswordChangeIntegrationTest extends ApiIntegrationTestSupport {

    private int change(Account account, String body) throws Exception {
        UUID id = userRepository.findByEmail(account.email()).orElseThrow().getId();
        return mockMvc.perform(put("/user/" + id)
                        .header("Authorization", account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void semASenhaAtualATrocaEhRecusada() throws Exception {
        var account = registerAndLogin();

        UUID id = userRepository.findByEmail(account.email()).orElseThrow().getId();
        mockMvc.perform(put("/user/" + id)
                        .header("Authorization", account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"senha-nova-123"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].field").value("currentPassword"));

        assertEquals(200, loginStatus(account.email(), account.password()));
    }

    @Test
    void comASenhaAtualErradaATrocaEhRecusada() throws Exception {
        var account = registerAndLogin();

        assertEquals(422, change(account, """
                {"password":"senha-nova-123","currentPassword":"errada-12345"}
                """));
        assertEquals(200, loginStatus(account.email(), account.password()));
    }

    @Test
    void trocaValidaMudaASenhaRevogaARenovacaoEAvisaPorEmail() throws Exception {
        var account = registerAndLogin();

        assertEquals(200, change(account, """
                {"password":"senha-nova-123","currentPassword":"%s"}
                """.formatted(account.password())));

        assertEquals(200, loginStatus(account.email(), "senha-nova-123"));
        assertEquals(401, loginStatus(account.email(), account.password()));
        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(account.refreshToken())))
                .andExpect(status().isUnauthorized());
        long notices = PasswordResetIntegrationTest.RecordingMail.SENT.stream()
                .filter(s -> s.to().equals(account.email()) && s.subject().equals("Sua senha foi alterada"))
                .count();
        assertEquals(1, notices);
    }

    @Test
    void atualizarSoONomeNaoPedeSenha() throws Exception {
        var account = registerAndLogin();

        assertEquals(200, change(account, """
                {"name":"Novo Nome"}
                """));
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

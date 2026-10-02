package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(PasswordResetIntegrationTest.RecordingMail.class)
class RegistrationEnumerationIntegrationTest extends ApiIntegrationTestSupport {

    private MvcResult register(String email, String name, String password) throws Exception {
        return mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","user":"%s","password":"%s"}
                                """.formatted(email, name, password)))
                .andReturn();
    }

    private int login(String email, String password) throws Exception {
        return mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().is(org.hamcrest.Matchers.anything()))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void emailNovoEEmailJaCadastradoRecebemExatamenteAMesmaResposta() throws Exception {
        String existing = "ja-existe-" + UUID.randomUUID() + "@test.com";
        String fresh = "novo-" + UUID.randomUUID() + "@test.com";
        register(existing, "Dono", "senha-do-dono-1");

        MvcResult forExisting = register(existing, "Intruso", "senha-do-intruso-1");
        MvcResult forFresh = register(fresh, "Novo", "senha-segura-1");

        assertEquals(forFresh.getResponse().getStatus(), forExisting.getResponse().getStatus());
        assertEquals(201, forExisting.getResponse().getStatus());
        assertEquals(forFresh.getResponse().getContentAsString(), forExisting.getResponse().getContentAsString());
    }

    @Test
    void cadastroSobreEmailExistenteNaoMudaASenhaNemCriaConta() throws Exception {
        String existing = "dono-" + UUID.randomUUID() + "@test.com";
        register(existing, "Dono", "senha-do-dono-1");

        register(existing, "Intruso", "senha-do-intruso-1");

        assertEquals(200, login(existing, "senha-do-dono-1"));
        assertEquals(401, login(existing, "senha-do-intruso-1"));
        assertEquals(1, userRepository.findAll().stream().filter(u -> u.getEmail().equals(existing)).count());
    }

    @Test
    void odonoRecebeUmAvisoComOLinkParaRecuperarASenha() throws Exception {
        String existing = "aviso-" + UUID.randomUUID() + "@test.com";
        register(existing, "Dono", "senha-do-dono-1");

        register(existing, "Intruso", "senha-do-intruso-1");

        long notices = PasswordResetIntegrationTest.RecordingMail.SENT.stream()
                .filter(s -> s.to().equals(existing) && s.subject().startsWith("Alguém tentou criar uma conta"))
                .count();
        assertEquals(1, notices);
    }
}

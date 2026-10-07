package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserValidationMessagesIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void cadastroComNomeAcimaDoLimiteMostraOLimiteCerto() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nome-%s@test.com","user":"%s","password":"senha-segura-1"}
                                """.formatted(UUID.randomUUID(), "a".repeat(21))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].field").value("user"))
                .andExpect(jsonPath("$.fieldsError[0].message").value("O nome deve ter no máximo 20 caracteres"));
    }

    @Test
    void cadastroComNomeEmBrancoEhRecusado() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"branco-%s@test.com","user":"   ","password":"senha-segura-1"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].message").value("O nome não pode ficar vazio"));
    }

    @Test
    void senhaCurtaMostraOIntervaloPermitido() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"curta-%s@test.com","user":"Caio","password":"1234567"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].message").value("A senha deve ter entre 8 e 100 caracteres"));
    }

    @Test
    void atualizarComNomeVazioEhRecusado() throws Exception {
        Account account = registerAndLogin();
        String id = userRepository.findByEmail(account.email()).orElseThrow().getId().toString();

        mockMvc.perform(put("/user/" + id)
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].field").value("name"));
    }
}

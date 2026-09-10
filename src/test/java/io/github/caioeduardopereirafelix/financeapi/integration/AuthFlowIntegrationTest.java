package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void loginDeveDevolverAccessTokenERefreshToken() throws Exception {
        var account = registerAndLogin();

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(account.email(), account.password())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void cadastroComEmailRepetidoDeveResponder409() throws Exception {
        var account = registerAndLogin();

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","user":"Outro","password":"123456"}
                                """.formatted(account.email())))
                .andExpect(status().isConflict());
    }

    @Test
    void loginComSenhaErradaDeveResponder401() throws Exception {
        var account = registerAndLogin();

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"senha-errada"}
                                """.formatted(account.email())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoSemTokenDeveResponder401() throws Exception {
        mockMvc.perform(get("/transaction"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshDeveEmitirParNovoEInvalidarOTokenAntigo() throws Exception {
        var account = registerAndLogin();

        String novoRefresh = objectMapper.readTree(
                        mockMvc.perform(post("/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                                {"refreshToken":"%s"}
                                                """.formatted(account.refreshToken())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").isNotEmpty())
                                .andReturn().getResponse().getContentAsString())
                .get("refreshToken").asText();

        // rotacao: o token apresentado nao serve mais
        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(account.refreshToken())))
                .andExpect(status().isUnauthorized());

        // o novo, sim
        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(novoRefresh)))
                .andExpect(status().isOk());
    }

    @Test
    void logoutDeveRevogarORefreshToken() throws Exception {
        var account = registerAndLogin();

        mockMvc.perform(post("/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(account.refreshToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(account.refreshToken())))
                .andExpect(status().isUnauthorized());
    }
}

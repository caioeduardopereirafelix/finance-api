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
    void cadastroComEmailRepetidoResponde201ComoQualquerOutro() throws Exception {
        var account = registerAndLogin();

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","user":"Outro","password":"senha-segura-1"}
                                """.formatted(account.email())))
                .andExpect(status().isCreated());
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

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(account.refreshToken())))
                .andExpect(status().isUnauthorized());

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

    @Test
    void cadastroComSenhaCurtaDeveResponder422() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"curta-%s@test.com","user":"Curta","password":"1234567"}
                                """.formatted(java.util.UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void loginNaoExigeTamanhoMinimoParaNaoTrancarContasAntigas() throws Exception {
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"antigo-%s@test.com","password":"123"}
                                """.formatted(java.util.UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void depoisDeVariasSenhasErradasOLoginDeveSerTravadoInclusiveComASenhaCerta() throws Exception {
        var account = registerAndLogin();
        String errada = """
                {"email":"%s","password":"senha-errada"}
                """.formatted(account.email());

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(errada))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(errada))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.startsWith("Muitas tentativas")));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(account.email(), account.password())))
                .andExpect(status().isTooManyRequests());
    }
}

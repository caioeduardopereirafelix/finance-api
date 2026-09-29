package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sem nenhum provedor habilitado (o padrao), a integracao responde 503 em vez de fingir que funciona. */
class BankWithoutProviderIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void semProvedorConfiguradoDeveResponder503() throws Exception {
        var a = registerAndLogin();

        mockMvc.perform(post("/bank/connect-token").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isServiceUnavailable());

        mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"qualquer\"}"))
                .andExpect(status().isServiceUnavailable());
    }
}

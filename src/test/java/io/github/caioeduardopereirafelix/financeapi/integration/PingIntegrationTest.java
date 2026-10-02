package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PingIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void pingRespondeSemAutenticacaoEOuvidoNaoGuardaCache() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void pingAceitaHead() throws Exception {
        mockMvc.perform(head("/ping")).andExpect(status().isOk());
    }
}

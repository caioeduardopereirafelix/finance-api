package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionAmountValidationIntegrationTest extends ApiIntegrationTestSupport {

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10.00", "null"})
    void valorZeroNegativoOuAusenteEhRecusadoNaEntrada(String amount) throws Exception {
        Account account = registerAndLogin();

        mockMvc.perform(post("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"EXPENSES","category":"FOOD","amount":%s,"description":"Teste"}
                                """.formatted(amount)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].field").value("amount"));
    }
}

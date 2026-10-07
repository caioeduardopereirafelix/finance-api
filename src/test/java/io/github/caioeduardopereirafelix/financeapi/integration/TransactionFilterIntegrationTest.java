package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionFilterIntegrationTest extends ApiIntegrationTestSupport {

    private void create(Account account, String type, String category, String amount, String description) throws Exception {
        mockMvc.perform(post("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"%s","category":"%s","amount":%s,"description":"%s"}
                                """.formatted(type, category, amount, description)))
                .andExpect(status().isCreated());
    }

    @Test
    void osFiltrosDaListagemChegamJuntosAoService() throws Exception {
        Account account = registerAndLogin();
        create(account, "EXPENSES", "FOOD", "50.00", "Padaria do bairro");
        create(account, "EXPENSES", "FOOD", "150.00", "Restaurante");
        create(account, "CASH_ENTRY", "WAGE", "3000.00", "Salario");

        mockMvc.perform(get("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .param("type", "EXPENSES")
                        .param("category", "FOOD")
                        .param("minAmount", "100")
                        .param("maxAmount", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Restaurante"));

        mockMvc.perform(get("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .param("description", "padaria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void filtroComValorInvalidoEhRecusado() throws Exception {
        Account account = registerAndLogin();

        mockMvc.perform(get("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .param("type", "NAO_EXISTE"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].field").value("type"))
                .andExpect(jsonPath("$.fieldsError[0].message").value("Valor invalido"));
    }
}

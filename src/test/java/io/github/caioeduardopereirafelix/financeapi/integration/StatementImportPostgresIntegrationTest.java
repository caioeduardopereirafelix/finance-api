package io.github.caioeduardopereirafelix.financeapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatementImportPostgresIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    private static String csv(int rows) {
        StringBuilder sb = new StringBuilder("Data;Descrição;Valor\n");
        for (int i = 0; i < rows; i++) {
            int day = 1 + (i % 28);
            sb.append(String.format("%02d/09/2026;Compra numero %d;-%d,%02d%n", day, i, 1 + (i % 500), i % 100));
        }
        return sb.toString();
    }

    private JsonNode send(Account a, String content, int expectedStatus) throws Exception {
        String body = mockMvc.perform(multipart("/transaction/import")
                        .file(new MockMultipartFile("file", "extrato.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8)))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private long storedFor(Account a) {
        return jdbc.queryForObject("""
                select count(*) from transactions t join users u on u.id = t.user_id
                where u.email = ? and t.source = 'FILE'
                """, Long.class, a.email());
    }

    @Test
    void importaCincoMilLinhasEODeduplicaNaSegundaVezContraOPostgres() throws Exception {
        var a = registerAndLogin();
        String content = csv(5000);

        long start = System.nanoTime();
        JsonNode first = send(a, content, 200);
        long firstMs = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        JsonNode second = send(a, content, 200);
        long secondMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("IMPORT 5000 linhas: primeira " + firstMs + " ms, repetida " + secondMs + " ms");

        assertEquals(5000, first.get("imported").asInt());
        assertEquals(0, first.get("skipped").asInt());
        assertEquals(0, second.get("imported").asInt());
        assertEquals(5000, second.get("skipped").asInt());
        assertEquals(5000, storedFor(a));
        assertTrue(firstMs < 30_000, "importacao muito lenta: " + firstMs + " ms");
    }

    @Test
    void maisDeCincoMilLancamentosEhRecusado() throws Exception {
        var a = registerAndLogin();

        send(a, csv(5001), 422);

        assertEquals(0, storedFor(a));
    }

    @Test
    void duasContasPodemTerOMesmoLancamentoPorqueOIndiceEPorUsuario() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();
        String content = csv(10);

        send(a, content, 200);
        JsonNode forB = send(b, content, 200);

        assertEquals(10, forB.get("imported").asInt());
        assertEquals(10, storedFor(a));
        assertEquals(10, storedFor(b));
    }
}

package io.github.caioeduardopereirafelix.financeapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O que so um PostgreSQL de verdade prova: as migrations rodam do zero, o Hibernate valida o
 * schema resultante, as restricoes existem e as consultas de periodo e soma se comportam.
 */
class PostgresIntegrationTest extends PostgresIntegrationTestSupport {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Flyway flyway;

    private UUID userId(Account a) {
        return jdbc.queryForObject("select id from users where email = ?", UUID.class, a.email());
    }

    private String criar(Account a, String description, String amount, String type, String category, String occurredOn)
            throws Exception {
        String date = occurredOn == null ? "" : ",\"occurredOn\":\"" + occurredOn + "\"";
        String body = mockMvc.perform(post("/transaction")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"%s\",\"amount\":%s,\"type\":\"%s\",\"category\":\"%s\"%s}"
                                .formatted(description, amount, type, category, date)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private void conectarESincronizar(Account a) throws Exception {
        String body = mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"conn-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk());
    }

    private JsonNode listar(Account a, String... params) throws Exception {
        var request = get("/transaction").header(HttpHeaders.AUTHORIZATION, a.bearer()).param("size", "50");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        String body = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("content");
    }

    private long contar(String table, UUID userId) {
        return jdbc.queryForObject("select count(*) from " + table + " where user_id = ?", Long.class, userId);
    }

    private String diasAtras(int dias) {
        return LocalDate.now(ZONE).minusDays(dias).toString();
    }

    @Test
    void asMigrationsRodamDoZeroSemPendenciaNemFalha() {
        MigrationInfo[] all = flyway.info().all();

        assertTrue(all.length >= 7, "esperava ao menos as migrations V1 a V7");
        assertTrue(Arrays.stream(all).allMatch(m -> m.getState() == MigrationState.SUCCESS),
                "toda migration deve estar aplicada com sucesso: " + Arrays.toString(all));
        assertEquals(0, flyway.info().pending().length);
    }

    @Test
    void oSchemaTemAsRestricoesEIndicesQueOCodigoAssume() {
        List<String> indexes = jdbc.queryForList(
                "select indexname from pg_indexes where schemaname = current_schema()", String.class);
        assertTrue(indexes.containsAll(List.of(
                "uk_transactions_user_external", "idx_transactions_user_occurred_at",
                "idx_bank_connections_user", "idx_refresh_tokens_user")), indexes.toString());

        List<String> constraints = jdbc.queryForList(
                "select constraint_name from information_schema.table_constraints"
                        + " where constraint_schema = current_schema()", String.class);
        assertTrue(constraints.containsAll(List.of(
                "uk_users_email", "uk_bank_connections_provider_external", "uk_category_rules",
                "uk_refresh_tokens_token_hash")), constraints.toString());

        for (String fk : List.of("fk_transactions_user", "fk_bank_connections_user",
                "fk_category_rules_user", "fk_refresh_tokens_user")) {
            String rule = jdbc.queryForObject(
                    "select delete_rule from information_schema.referential_constraints"
                            + " where constraint_name = ? and constraint_schema = current_schema()", String.class, fk);
            assertEquals("CASCADE", rule, fk);
        }

        assertEquals("SET NULL", jdbc.queryForObject(
                "select delete_rule from information_schema.referential_constraints"
                        + " where constraint_name = 'fk_transactions_bank_connection'", String.class));

        assertEquals("timestamp with time zone", jdbc.queryForObject(
                "select data_type from information_schema.columns"
                        + " where table_name = 'transactions' and column_name = 'occurred_at'", String.class));
        assertEquals("NO", jdbc.queryForObject(
                "select is_nullable from information_schema.columns"
                        + " where table_name = 'transactions' and column_name = 'occurred_at'", String.class));
    }

    @Test
    void oBancoRecusaDuasTransacoesComOMesmoIdExternoDoMesmoUsuario() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();
        UUID idA = userId(a);
        UUID idB = userId(b);
        String sql = "insert into transactions (id, description, amount, type, category, user_id, created_date,"
                + " occurred_at, external_id) values (?, 'x', 1.00, 'EXPENSES', 'FOOD', ?, now(), now(), ?)";
        String externalId = "ext-" + UUID.randomUUID();

        jdbc.update(sql, UUID.randomUUID(), idA, externalId);

        assertThrows(DuplicateKeyException.class, () -> jdbc.update(sql, UUID.randomUUID(), idA, externalId));
        jdbc.update(sql, UUID.randomUUID(), idB, externalId);
    }

    @Test
    void sincronizarDeNovoNaoDuplicaAsImportadas() throws Exception {
        var a = registerAndLogin();
        String body = mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"conn-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(body).get("id").asText();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                    .andExpect(status().isOk());
        }

        assertEquals(7, contar("transactions", userId(a)));
    }

    @Test
    void oPeriodoUsaODiaDeSaoPauloEIncluiOUltimoDia() throws Exception {
        var a = registerAndLogin();
        UUID id = userId(a);
        ZonedDateTime day = LocalDate.now(ZONE).minusDays(5).atStartOfDay(ZONE);
        String sql = "insert into transactions (id, description, amount, type, category, user_id, created_date,"
                + " occurred_at) values (?, ?, ?, 'EXPENSES', 'FOOD', ?, now(), ?)";

        // 00:30 e 23:30 de Sao Paulo: em UTC o segundo ja cai no dia seguinte
        jdbc.update(sql, UUID.randomUUID(), "inicio-do-dia", 10, id, day.plusMinutes(30).toOffsetDateTime());
        jdbc.update(sql, UUID.randomUUID(), "fim-do-dia", 20, id, day.plusHours(23).plusMinutes(30).toOffsetDateTime());
        jdbc.update(sql, UUID.randomUUID(), "dia-anterior", 40, id, day.minusMinutes(30).toOffsetDateTime());
        jdbc.update(sql, UUID.randomUUID(), "dia-seguinte", 80, id, day.plusHours(24).plusMinutes(30).toOffsetDateTime());

        String dia = day.toLocalDate().toString();
        JsonNode doDia = listar(a, "startDate", dia, "endDate", dia);

        assertEquals(2, doDia.size());
        mockMvc.perform(get("/transaction/summary").param("startDate", dia).param("endDate", dia)
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.expenses").value(30.00))
                .andExpect(jsonPath("$.balance").value(-30.00));
    }

    @Test
    void aListaVemDaMaisRecenteParaAMaisAntigaPelaDataEscolhida() throws Exception {
        var a = registerAndLogin();
        criar(a, "do-meio", "1", "EXPENSES", "FOOD", diasAtras(10));
        criar(a, "antiga", "1", "EXPENSES", "FOOD", diasAtras(30));
        criar(a, "recente", "1", "EXPENSES", "FOOD", diasAtras(1));

        JsonNode lista = listar(a);

        assertEquals("recente", lista.get(0).get("description").asText());
        assertEquals("do-meio", lista.get(1).get("description").asText());
        assertEquals("antiga", lista.get(2).get("description").asText());
    }

    @Test
    void asSomasSaoExatasEOrdenadasPeloMaiorTotal() throws Exception {
        var a = registerAndLogin();
        criar(a, "a", "0.10", "EXPENSES", "FOOD", null);
        criar(a, "b", "0.20", "EXPENSES", "FOOD", null);
        criar(a, "c", "0.29", "EXPENSES", "HEALTH", null);
        criar(a, "d", "1000.05", "CASH_ENTRY", "WAGE", null);

        mockMvc.perform(get("/transaction/summary").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.cashEntry").value(1000.05))
                .andExpect(jsonPath("$.expenses").value(0.59))
                .andExpect(jsonPath("$.balance").value(999.46));

        mockMvc.perform(get("/transaction/summary/by-category").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].category").value("WAGE"))
                .andExpect(jsonPath("$[0].total").value(1000.05))
                .andExpect(jsonPath("$[1].category").value("FOOD"))
                .andExpect(jsonPath("$[1].total").value(0.30))
                .andExpect(jsonPath("$[1].count").value(2))
                .andExpect(jsonPath("$[2].category").value("HEALTH"));
    }

    @Test
    void umUsuarioNaoEnxergaNemSomaOsDadosDeOutro() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();
        criar(a, "so-do-a", "10", "EXPENSES", "FOOD", null);

        assertEquals(0, listar(b).size());
        mockMvc.perform(get("/transaction/summary").header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(jsonPath("$.expenses").value(0));
    }

    @Test
    void recategorizarGuardaUmaSoRegraPorEstabelecimentoEAsProximasImportacoesObedecem() throws Exception {
        var a = registerAndLogin();
        conectarESincronizar(a);
        String tarifa = listar(a, "description", "Tarifa").get(0).get("id").asText();

        for (String category : List.of("BILLS", "HEALTH", "BILLS")) {
            mockMvc.perform(patch("/transaction/" + tarifa + "/category")
                            .header(HttpHeaders.AUTHORIZATION, a.bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"category\":\"" + category + "\",\"applyToSimilar\":true}"))
                    .andExpect(status().isOk());
        }

        assertEquals(1, contar("category_rules", userId(a)));

        conectarESincronizar(a);
        assertEquals(2, listar(a, "description", "Tarifa", "category", "BILLS").size());
    }

    @Test
    void apagarAContaLimpaTudoDoUsuarioEDeixaOsOutrosIntactos() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();
        criar(a, "manual", "10", "EXPENSES", "FOOD", null);
        conectarESincronizar(a);
        conectarESincronizar(b);
        String tarifa = listar(a, "description", "Tarifa").get(0).get("id").asText();
        mockMvc.perform(patch("/transaction/" + tarifa + "/category")
                .header(HttpHeaders.AUTHORIZATION, a.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"BILLS\",\"applyToSimilar\":true}"));
        UUID idA = userId(a);
        UUID idB = userId(b);
        assertTrue(contar("transactions", idA) > 0);
        assertTrue(contar("bank_connections", idA) > 0);
        assertTrue(contar("category_rules", idA) > 0);
        assertTrue(contar("refresh_tokens", idA) > 0);
        long doB = contar("transactions", idB);

        mockMvc.perform(delete("/user/" + idA).header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isNoContent());

        for (String table : List.of("transactions", "bank_connections", "category_rules", "refresh_tokens")) {
            assertEquals(0, contar(table, idA), table);
        }
        assertEquals(0, jdbc.queryForObject("select count(*) from users where id = ?", Long.class, idA));
        assertEquals(doB, contar("transactions", idB));
    }
}

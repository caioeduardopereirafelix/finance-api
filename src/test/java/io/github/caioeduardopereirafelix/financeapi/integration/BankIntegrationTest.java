package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "bank.mock.enabled=true")
class BankIntegrationTest extends ApiIntegrationTestSupport {

    private String conectar(Account account, String externalId) throws Exception {
        String body = mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, account.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"%s\"}".formatted(externalId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institutionName").value("Banco Demo"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.externalId").doesNotExist())   // nao vaza o id do provedor
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String novoExternalId() {
        return "conn-" + UUID.randomUUID();
    }

    @Test
    void semTokenNaoDeveAcessarAApi() throws Exception {
        mockMvc.perform(get("/bank/connections")).andExpect(status().isUnauthorized());
    }

    @Test
    void deveEmitirTokenDeConexao() throws Exception {
        var a = registerAndLogin();

        mockMvc.perform(post("/bank/connect-token").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.provider").value("mock"));
    }

    @Test
    void sincronizacaoDeveImportarEClassificarAsTransacoes() throws Exception {
        var a = registerAndLogin();
        String id = conectar(a, novoExternalId());

        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(7))
                .andExpect(jsonPath("$.skipped").value(0));

        // 5 saidas e 2 entradas no provedor de mentira
        mockMvc.perform(get("/transaction").param("size", "20").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(7));

        mockMvc.perform(get("/transaction").param("category", "FOOD").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Supermercado Central"));
    }

    @Test
    void sincronizarDeNovoNaoDeveDuplicar() throws Exception {
        var a = registerAndLogin();
        String id = conectar(a, novoExternalId());

        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.imported").value(7));

        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(0))
                // a segunda busca so cobre os ultimos 7 dias antes da anterior: 3 dos 7 itens
                .andExpect(jsonPath("$.skipped").value(3));

        mockMvc.perform(get("/transaction").param("size", "20").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(7));
    }

    @Test
    void resumoDeveConsiderarAsTransacoesImportadas() throws Exception {
        var a = registerAndLogin();
        String id = conectar(a, novoExternalId());
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()));

        // entradas: 5400 + 150 | saidas: 312.48 + 27.90 + 84.10 + 39.90 + 9.90
        mockMvc.perform(get("/transaction/summary").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.cashEntry").value(5550.00))
                .andExpect(jsonPath("$.expenses").value(474.28))
                .andExpect(jsonPath("$.balance").value(5075.72));
    }

    @Test
    void usuarioNaoPodeVerNemSincronizarNemApagarConexaoDeOutro() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();
        String idDoA = conectar(a, novoExternalId());

        mockMvc.perform(post("/bank/connections/" + idDoA + "/sync").header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/bank/connections/" + idDoA).header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/bank/connections").header(HttpHeaders.AUTHORIZATION, b.bearer()))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/bank/connections").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void naoDeveCadastrarAMesmaConexaoDuasVezes() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();
        String externalId = novoExternalId();
        conectar(a, externalId);

        mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, b.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"%s\"}".formatted(externalId)))
                .andExpect(status().isConflict());
    }

    @Test
    void externalIdVazioDeveSerRejeitado() throws Exception {
        var a = registerAndLogin();

        mockMvc.perform(post("/bank/connections")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalId\":\"\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void desconectarMantemOHistoricoPorPadrao() throws Exception {
        var a = registerAndLogin();
        String id = conectar(a, novoExternalId());
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()));

        mockMvc.perform(delete("/bank/connections/" + id).header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/transaction").param("size", "20").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(7));
    }

    @Test
    void desconectarComDeleteImportedApagaAsTransacoesDaConexao() throws Exception {
        var a = registerAndLogin();
        String id = conectar(a, novoExternalId());
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()));

        // um lancamento manual que nao pode ser apagado junto
        mockMvc.perform(post("/transaction")
                .header(HttpHeaders.AUTHORIZATION, a.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Manual\",\"amount\":10.00,\"type\":\"EXPENSES\",\"category\":\"FOOD\"}"));

        mockMvc.perform(delete("/bank/connections/" + id).param("deleteImported", "true")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/transaction").param("size", "20").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Manual"));
    }

    private static final java.time.ZoneId ZONE = java.time.ZoneId.of("America/Sao_Paulo");

    private String dia(int diasAtras) {
        return java.time.LocalDate.now(ZONE).minusDays(diasAtras).toString();
    }

    @Test
    void naoReconhecidasViramOutrosEPixSemCategoriaTambem() throws Exception {
        var a = registerAndLogin();
        String id = conectar(a, novoExternalId());
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()));

        mockMvc.perform(get("/transaction").param("category", "OTHER_EXPENSE").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Tarifa desconhecida"));

        mockMvc.perform(get("/transaction").param("category", "OTHER_INCOME").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Pix recebido"));
    }

    @Test
    void listagemDeveOrdenarPelaDataDoGastoENaoPeloMomentoDaImportacao() throws Exception {
        var a = registerAndLogin();
        String id = conectar(a, novoExternalId());
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()));

        // tudo entrou no mesmo instante; o que ordena e a data em que o gasto ocorreu
        mockMvc.perform(get("/transaction").param("size", "20").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.content[0].description").value("Supermercado Central"))      // 2 dias atras
                .andExpect(jsonPath("$.content[6].description").value("Tarifa desconhecida"));      // 20 dias atras
    }

    @Test
    void respostaDeveTrazerADataDoGastoEAOrigem() throws Exception {
        var a = registerAndLogin();
        String id = conectar(a, novoExternalId());
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()));

        mockMvc.perform(get("/transaction").param("category", "FOOD").header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.content[0].source").value("BANK"))
                .andExpect(jsonPath("$.content[0].occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.content[0].createdDate").isNotEmpty());
    }

    @Test
    void filtroDePeriodoDeveUsarADataDoGasto() throws Exception {
        var a = registerAndLogin();
        String id = conectar(a, novoExternalId());
        mockMvc.perform(post("/bank/connections/" + id + "/sync").header(HttpHeaders.AUTHORIZATION, a.bearer()));

        // gastos de 2, 3 e 5 dias atras (7 itens no total, todos importados agora)
        mockMvc.perform(get("/transaction").param("startDate", dia(6)).param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(3));

        // entre 13 e 9 dias atras: os de 10 e 12 dias
        mockMvc.perform(get("/transaction").param("startDate", dia(13)).param("endDate", dia(9)).param("size", "20")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void oDiaFinalDoFiltroEInclusivo() throws Exception {
        var a = registerAndLogin();
        mockMvc.perform(post("/transaction")
                .header(HttpHeaders.AUTHORIZATION, a.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Hoje\",\"amount\":10.00,\"type\":\"EXPENSES\",\"category\":\"FOOD\"}"));

        // endDate = hoje: o lancamento de agora precisa aparecer
        mockMvc.perform(get("/transaction").param("endDate", dia(0)).header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(1));

        // endDate = ontem: nao pode
        mockMvc.perform(get("/transaction").param("endDate", dia(1)).header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void lancamentoManualTemADataDoGastoIgualAoMomentoDoLancamento() throws Exception {
        var a = registerAndLogin();
        mockMvc.perform(post("/transaction")
                .header(HttpHeaders.AUTHORIZATION, a.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Manual\",\"amount\":10.00,\"type\":\"EXPENSES\",\"category\":\"OTHER_EXPENSE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.occurredAt").isNotEmpty())
                .andExpect(jsonPath("$.source").value("MANUAL"))
                .andExpect(jsonPath("$.category").value("OTHER_EXPENSE"));
    }

    @Test
    void categoriaOutrosNaoPodeSerUsadaComOTipoErrado() throws Exception {
        var a = registerAndLogin();
        mockMvc.perform(post("/transaction")
                .header(HttpHeaders.AUTHORIZATION, a.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"x\",\"amount\":10.00,\"type\":\"CASH_ENTRY\",\"category\":\"OTHER_EXPENSE\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}

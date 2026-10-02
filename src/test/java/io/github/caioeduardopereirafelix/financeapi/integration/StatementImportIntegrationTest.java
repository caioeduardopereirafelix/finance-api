package io.github.caioeduardopereirafelix.financeapi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatementImportIntegrationTest extends ApiIntegrationTestSupport {

    private static final String OFX = """
            OFXHEADER:100
            DATA:OFXSGML
            <OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS>
            <BANKACCTFROM><BANKID>0260<ACCTID>12345-6</BANKACCTFROM>
            <BANKTRANLIST>
            <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260915000000[-3:BRT]<TRNAMT>-42.50<FITID>A1<MEMO>PADARIA SOL 001</STMTTRN>
            <STMTTRN><TRNTYPE>CREDIT<DTPOSTED>20260920<TRNAMT>3500.00<FITID>A2<MEMO>CREDITO SALARIO</STMTTRN>
            <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260921<TRNAMT>-80.00<FITID>A3<MEMO>PADARIA SOL 002</STMTTRN>
            </BANKTRANLIST>
            </STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
            """;

    private static final String OFX_OVERLAP = """
            <OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS>
            <BANKACCTFROM><BANKID>0260<ACCTID>12345-6</BANKACCTFROM>
            <BANKTRANLIST>
            <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260921<TRNAMT>-80.00<FITID>A3<MEMO>PADARIA SOL 002</STMTTRN>
            <STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260925<TRNAMT>-10.00<FITID>A4<MEMO>BANCA DE JORNAL</STMTTRN>
            </BANKTRANLIST>
            </STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
            """;

    private MockMultipartFile file(String name, String content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode importFile(Account a, MockMultipartFile file, boolean invertSign) throws Exception {
        String body = mockMvc.perform(multipart("/transaction/import").file(file)
                        .param("invertSign", String.valueOf(invertSign))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private JsonNode list(Account a) throws Exception {
        String body = mockMvc.perform(get("/transaction").param("size", "50")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("content");
    }

    private String idOf(Account a, String descriptionPart) throws Exception {
        for (JsonNode item : list(a)) {
            if (item.get("description").asText().contains(descriptionPart)) {
                return item.get("id").asText();
            }
        }
        throw new AssertionError("nao achei " + descriptionPart);
    }

    @Test
    void semLoginAImportacaoEhRecusada() throws Exception {
        mockMvc.perform(multipart("/transaction/import").file(file("extrato.ofx", OFX)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void importaUmOfxEAsTransacoesAparecemComOrigemArquivo() throws Exception {
        var a = registerAndLogin();

        JsonNode result = importFile(a, file("extrato.ofx", OFX), false);

        assertEquals(3, result.get("total").asInt());
        assertEquals(3, result.get("imported").asInt());
        assertEquals(0, result.get("skipped").asInt());
        assertEquals(0, result.get("invalid").asInt());

        JsonNode items = list(a);
        assertEquals(3, items.size());
        for (JsonNode item : items) {
            assertEquals("FILE", item.get("source").asText());
        }
        JsonNode salary = null;
        for (JsonNode item : items) {
            if (item.get("description").asText().contains("SALARIO")) {
                salary = item;
            }
        }
        assertEquals("CASH_ENTRY", salary.get("type").asText());
        assertEquals("OTHER_INCOME", salary.get("category").asText());
        assertEquals(3500.00, salary.get("amount").asDouble(), 0.001);
    }

    @Test
    void importarDeNovoOMesmoArquivoNaoDuplicaNada() throws Exception {
        var a = registerAndLogin();
        importFile(a, file("extrato.ofx", OFX), false);

        JsonNode again = importFile(a, file("extrato.ofx", OFX), false);

        assertEquals(0, again.get("imported").asInt());
        assertEquals(3, again.get("skipped").asInt());
        assertEquals(3, list(a).size());
    }

    @Test
    void arquivoQueSobrepoeOPeriodoImportaSoOQueEhNovo() throws Exception {
        var a = registerAndLogin();
        importFile(a, file("setembro.ofx", OFX), false);

        JsonNode result = importFile(a, file("fim-de-setembro.ofx", OFX_OVERLAP), false);

        assertEquals(1, result.get("imported").asInt());
        assertEquals(1, result.get("skipped").asInt());
        assertEquals(4, list(a).size());
    }

    @Test
    void mesmoArquivoImportadoPorDoisUsuariosNaoSeMistura() throws Exception {
        var a = registerAndLogin();
        var b = registerAndLogin();

        importFile(a, file("extrato.ofx", OFX), false);
        JsonNode forB = importFile(b, file("extrato.ofx", OFX), false);

        assertEquals(3, forB.get("imported").asInt());
        assertEquals(3, list(a).size());
        assertEquals(3, list(b).size());
    }

    @Test
    void importaCsvBrasileiroEInverteOSinalQuandoPedido() throws Exception {
        var a = registerAndLogin();
        String csv = "Data;Descrição;Valor\n15/09/2026;Mercado Bom Preço;25,90\n16/09/2026;Reembolso;-10,00\n";

        JsonNode result = importFile(a, file("cartao.csv", csv), true);

        assertEquals(2, result.get("imported").asInt());
        for (JsonNode item : list(a)) {
            if (item.get("description").asText().contains("Mercado")) {
                assertEquals("EXPENSES", item.get("type").asText());
            } else {
                assertEquals("CASH_ENTRY", item.get("type").asText());
            }
        }
    }

    @Test
    void linhasRuinsSaoContadasEOsProblemasVoltamNaResposta() throws Exception {
        var a = registerAndLogin();
        String csv = "Data;Descrição;Valor\n15/09/2026;Boa;-1,00\nontem;Ruim;-2,00\n";

        JsonNode result = importFile(a, file("misto.csv", csv), false);

        assertEquals(1, result.get("imported").asInt());
        assertEquals(1, result.get("invalid").asInt());
        assertTrue(result.get("problems").get(0).asText().startsWith("Linha"));
    }

    @Test
    void arquivoQueNaoEUmExtratoEhRecusadoComOCampoFile() throws Exception {
        var a = registerAndLogin();

        mockMvc.perform(multipart("/transaction/import").file(file("foto.txt", "isto nao e um extrato\nde jeito nenhum\n"))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldsError[0].field").value("file"));
        mockMvc.perform(multipart("/transaction/import").file(file("vazio.csv", ""))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void arquivoMaiorQueOLimiteEhRecusadoCom413() throws Exception {
        var a = registerAndLogin();
        byte[] big = new byte[2 * 1024 * 1024 + 1024];
        java.util.Arrays.fill(big, (byte) 'a');

        mockMvc.perform(multipart("/transaction/import")
                        .file(new MockMultipartFile("file", "grande.csv", "text/csv", big))
                        .header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void transacaoImportadaDeArquivoSoAceitaMudarACategoria() throws Exception {
        var a = registerAndLogin();
        importFile(a, file("extrato.ofx", OFX), false);
        String id = idOf(a, "CREDITO SALARIO");

        mockMvc.perform(put("/transaction/" + id)
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"CREDITO SALARIO\",\"amount\":9999,\"type\":\"CASH_ENTRY\",\"category\":\"WAGE\"}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/transaction/" + id + "/category")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"WAGE\",\"applyToSimilar\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transaction.category").value("WAGE"));
    }

    @Test
    void recategorizarAplicandoNasParecidasValeParaOutrasImportadasDeArquivo() throws Exception {
        var a = registerAndLogin();
        importFile(a, file("extrato.ofx", OFX), false);
        String id = idOf(a, "PADARIA SOL 001");

        mockMvc.perform(patch("/transaction/" + id + "/category")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"FOOD\",\"applyToSimilar\":true}"))
                .andExpect(status().isOk());

        long food = 0;
        for (JsonNode item : list(a)) {
            if (item.get("category").asText().equals("FOOD")) {
                food++;
            }
        }
        assertEquals(2, food);
    }

    @Test
    void aRegraDoUsuarioSeAplicaNasProximasImportacoes() throws Exception {
        var a = registerAndLogin();
        importFile(a, file("extrato.ofx", OFX), false);
        String id = idOf(a, "PADARIA SOL 002");
        mockMvc.perform(patch("/transaction/" + id + "/category")
                        .header(HttpHeaders.AUTHORIZATION, a.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"FOOD\",\"applyToSimilar\":true}"))
                .andExpect(status().isOk());

        String next = "Data;Descrição;Valor\n30/09/2026;PADARIA SOL 003;-12,00\n";
        importFile(a, file("outubro.csv", next), false);

        boolean found = false;
        for (JsonNode item : list(a)) {
            if (item.get("description").asText().contains("003")) {
                assertEquals("FOOD", item.get("category").asText());
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void transacaoImportadaPodeSerApagada() throws Exception {
        var a = registerAndLogin();
        importFile(a, file("extrato.ofx", OFX), false);
        String id = idOf(a, "CREDITO SALARIO");

        mockMvc.perform(delete("/transaction/" + id).header(HttpHeaders.AUTHORIZATION, a.bearer()))
                .andExpect(status().is2xxSuccessful());

        assertEquals(2, list(a).size());
    }

    @Test
    void importarSemConfirmarOEmailEPermitido() throws Exception {
        var a = registerAndLoginUnverified();

        JsonNode result = importFile(a, file("extrato.ofx", OFX), false);

        assertEquals(3, result.get("imported").asInt());
    }
}

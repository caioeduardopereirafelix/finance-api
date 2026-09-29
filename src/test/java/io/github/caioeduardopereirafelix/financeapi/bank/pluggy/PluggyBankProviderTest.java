package io.github.caioeduardopereirafelix.financeapi.bank.pluggy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.caioeduardopereirafelix.financeapi.bank.BankIntegrationException;
import io.github.caioeduardopereirafelix.financeapi.bank.ExternalConnection;
import io.github.caioeduardopereirafelix.financeapi.bank.ExternalTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluggyBankProviderTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final String ITEM = "0b0d1d3e-1111-4222-8333-444455556666";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-29T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PluggyClient client;

    private PluggyBankProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PluggyBankProvider(client, false, SAO_PAULO, CLOCK);
    }

    private static JsonNode json(String text) {
        try {
            return JSON.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static JsonNode tx(String id, String type, String amount, String date, String status) {
        return json("""
                {"id":"%s","description":"Compra %s","amount":%s,"type":"%s","date":"%s","status":"%s","category":"Groceries"}
                """.formatted(id, id, amount, type, date, status));
    }

    @Test
    void nomeDeveSerPluggy() {
        assertEquals("pluggy", provider.name());
    }

    @Test
    void descreverConexaoDeveUsarONomeDoConector() {
        when(client.item(ITEM)).thenReturn(json("{\"id\":\"" + ITEM + "\",\"connector\":{\"name\":\"Nubank\"}}"));

        ExternalConnection c = provider.describeConnection(ITEM, "user-1");

        assertEquals(ITEM, c.externalId());
        assertEquals("Nubank", c.institutionName());
    }

    @Test
    void itemDeOutroUsuarioDeveSerRecusado() {
        when(client.item(ITEM)).thenReturn(json("{\"id\":\"" + ITEM + "\",\"clientUserId\":\"outro-usuario\",\"connector\":{\"name\":\"Nubank\"}}"));

        var e = assertThrows(BankIntegrationException.class, () -> provider.describeConnection(ITEM, "user-1"));

        assertEquals(403, e.getStatus().value());
    }

    @Test
    void itemDoProprioUsuarioDeveSerAceito() {
        when(client.item(ITEM)).thenReturn(json("{\"id\":\"" + ITEM + "\",\"clientUserId\":\"user-1\",\"connector\":{\"name\":\"Nubank\"}}"));

        assertEquals("Nubank", provider.describeConnection(ITEM, "user-1").institutionName());
    }

    @Test
    void itemSemClientUserIdEAceitoPoisNaoHaComoConferir() {
        when(client.item(ITEM)).thenReturn(json("{\"id\":\"" + ITEM + "\",\"connector\":{\"name\":\"Nubank\"}}"));

        assertEquals("Nubank", provider.describeConnection(ITEM, "user-1").institutionName());
    }

    @Test
    void reautorizarPedeATokenDeAtualizacaoDoItem() {
        when(client.createConnectToken("user-1", ITEM)).thenReturn("token-upd");

        assertEquals("token-upd", provider.createUpdateToken(ITEM, "user-1"));
    }

    @Test
    void reautorizarComIdentificadorInvalidoNaoChegaNaPluggy() {
        assertThrows(BankIntegrationException.class, () -> provider.createUpdateToken("../x", "user-1"));
        verify(client, never()).createConnectToken(any(), any());
    }

    @Test
    void desconectarDeveApagarOItemNaPluggy() {
        provider.disconnect(ITEM);

        verify(client).deleteItem(ITEM);
    }

    @Test
    void desconectarComIdentificadorInvalidoNaoChegaNaPluggy() {
        assertThrows(BankIntegrationException.class, () -> provider.disconnect("../items"));
        verify(client, never()).deleteItem(any());
    }

    @Test
    void identificadorQueNaoEUuidNaoDeveChegarNaPluggy() {
        var e = assertThrows(BankIntegrationException.class, () -> provider.describeConnection("../accounts", "user-1"));
        assertEquals(400, e.getStatus().value());
        verify(client, never()).item(any());
    }

    @Test
    void debitoViraSaidaECreditoViraEntradaIndependenteDoSinalDoValor() {
        when(client.accounts(ITEM)).thenReturn(List.of(json("{\"id\":\"a1\",\"type\":\"BANK\"}")));
        when(client.transactions("a1", LocalDate.parse("2026-03-01"))).thenReturn(List.of(
                tx("t1", "DEBIT", "-50.25", "2026-03-05T15:30:00.000Z", "POSTED"),
                tx("t2", "DEBIT", "50.25", "2026-03-05T15:30:00.000Z", "POSTED"),
                tx("t3", "CREDIT", "1000", "2026-03-06T10:00:00.000Z", "POSTED")));

        List<ExternalTransaction> result = provider.fetchTransactions(ITEM, Instant.parse("2026-03-01T12:00:00Z"));

        assertEquals(0, new BigDecimal("-50.25").compareTo(result.get(0).amount()));
        assertEquals(0, new BigDecimal("-50.25").compareTo(result.get(1).amount()));
        assertEquals(0, new BigDecimal("1000").compareTo(result.get(2).amount()));
        assertEquals("Groceries", result.get(0).category());
        assertEquals(Instant.parse("2026-03-05T15:30:00Z"), result.get(0).date());
    }

    @Test
    void movimentacaoPendenteNaoDeveSerImportada() {
        assertNull(provider.map(tx("t1", "DEBIT", "-10", "2026-03-05T15:30:00.000Z", "PENDING"), false));
    }

    @Test
    void dataSoComDiaNaoDeveCairNoDiaAnteriorNoFusoDoBrasil() {
        ExternalTransaction t = provider.map(tx("t1", "DEBIT", "-10", "2026-03-05T00:00:00.000Z", "POSTED"), false);

        assertEquals(LocalDate.parse("2026-03-05"), t.date().atZone(SAO_PAULO).toLocalDate());
    }

    @Test
    void dataSemHorarioTambemDeveSerAceita() {
        ExternalTransaction t = provider.map(tx("t1", "DEBIT", "-10", "2026-03-05", "POSTED"), false);

        assertEquals(LocalDate.parse("2026-03-05"), t.date().atZone(SAO_PAULO).toLocalDate());
    }

    @Test
    void registroIncompletoDeveSerIgnoradoEmVezDeQuebrarASincronizacao() {
        assertNull(provider.map(json("{\"id\":\"x\",\"type\":\"DEBIT\",\"date\":\"2026-03-05\"}"), false));
        assertNull(provider.map(json("{\"type\":\"DEBIT\",\"amount\":1,\"date\":\"2026-03-05\"}"), false));
        assertNull(provider.map(json("{\"id\":\"x\",\"type\":\"DEBIT\",\"amount\":1,\"date\":\"lixo\"}"), false));
    }

    @Test
    void semTypeVaiPeloSinalDoValor() {
        ExternalTransaction saida = provider.map(json(
                "{\"id\":\"a\",\"amount\":-8,\"date\":\"2026-03-05T10:00:00Z\"}"), false);
        assertEquals(0, new BigDecimal("-8").compareTo(saida.amount()));
    }

    @Test
    void descricaoVaziaUsaADescricaoBruta() {
        ExternalTransaction t = provider.map(json("""
                {"id":"a","description":"","descriptionRaw":"PIX ENVIADO","amount":-8,"type":"DEBIT","date":"2026-03-05T10:00:00Z"}
                """), false);
        assertEquals("PIX ENVIADO", t.description());
    }

    @Test
    void descricaoLongaDeveSerTruncadaParaCaberNaColuna() {
        String longa = "x".repeat(400);
        ExternalTransaction t = provider.map(json("""
                {"id":"a","description":"%s","amount":-8,"type":"DEBIT","date":"2026-03-05T10:00:00Z"}
                """.formatted(longa)), false);
        assertEquals(255, t.description().length());
    }

    @Test
    void cartaoDeCreditoFicaDeForaPorPadrao() {
        when(client.accounts(ITEM)).thenReturn(List.of(json("{\"id\":\"card\",\"type\":\"CREDIT\"}")));

        assertEquals(List.of(), provider.fetchTransactions(ITEM, Instant.parse("2026-03-01T00:00:00Z")));
        verify(client, never()).transactions(any(), any());
    }

    @Test
    void comCartaoLigadoEntramAsComprasEOsEstornosEFicamDeForaOPagamentoDaFatura() {
        provider = new PluggyBankProvider(client, true, SAO_PAULO, CLOCK);
        when(client.accounts(ITEM)).thenReturn(List.of(json("{\"id\":\"card\",\"type\":\"CREDIT\",\"subtype\":\"CREDIT_CARD\"}")));
        when(client.transactions(eq("card"), any())).thenReturn(List.of(
                tx("compra", "DEBIT", "120.00", "2026-03-05T15:30:00.000Z", "POSTED"),
                json("""
                        {"id":"estorno","description":"Estorno loja","amount":-30,"type":"CREDIT","operationType":"ESTORNO",
                         "date":"2026-03-07T10:00:00.000Z","status":"POSTED"}"""),
                json("""
                        {"id":"cashback","description":"Cashback","amount":-2.5,"type":"CREDIT","operationType":"CASHBACK",
                         "date":"2026-03-07T10:00:00.000Z","status":"POSTED"}"""),
                json("""
                        {"id":"pagamento-fatura","description":"Pagamento recebido","amount":-900,"type":"CREDIT",
                         "operationType":"PAGAMENTO_FATURA","date":"2026-03-06T10:00:00.000Z","status":"POSTED"}"""),
                tx("sem-operation-type", "CREDIT", "-40.00", "2026-03-06T10:00:00.000Z", "POSTED")));

        List<ExternalTransaction> result = provider.fetchTransactions(ITEM, Instant.parse("2026-03-01T00:00:00Z"));

        assertEquals(List.of("compra", "estorno", "cashback"), result.stream().map(ExternalTransaction::id).toList());
        assertEquals(0, new BigDecimal("-120.00").compareTo(result.get(0).amount()));
        assertEquals(0, new BigDecimal("30").compareTo(result.get(1).amount()));
        assertEquals(0, new BigDecimal("2.5").compareTo(result.get(2).amount()));
    }

    @Test
    void emprestimoNaoEntraMesmoComCartaoLigado() {
        provider = new PluggyBankProvider(client, true, SAO_PAULO, CLOCK);
        when(client.accounts(ITEM)).thenReturn(List.of(json("{\"id\":\"loan\",\"type\":\"CREDIT\",\"subtype\":\"LOAN\"}")));

        assertEquals(List.of(), provider.fetchTransactions(ITEM, Instant.parse("2026-03-01T00:00:00Z")));
        verify(client, never()).transactions(any(), any());
    }

    @Test
    void cartaoSempreBuscaUmaJanelaMaiorPorqueAFaturaAbertaFicaPendente() {
        provider = new PluggyBankProvider(client, true, SAO_PAULO, CLOCK);
        when(client.accounts(ITEM)).thenReturn(List.of(
                json("{\"id\":\"bank\",\"type\":\"BANK\"}"),
                json("{\"id\":\"card\",\"type\":\"CREDIT\",\"subtype\":\"CREDIT_CARD\"}")));
        when(client.transactions(eq("bank"), any())).thenReturn(List.of());
        when(client.transactions(eq("card"), any())).thenReturn(List.of());

        provider.fetchTransactions(ITEM, Instant.parse("2026-09-27T12:00:00Z"));

        verify(client).transactions("bank", LocalDate.parse("2026-09-27"));
        verify(client).transactions("card", LocalDate.parse("2026-07-31"));
    }

    @Test
    void valorEmMoedaEstrangeiraUsaOValorNaMoedaDaConta() {
        ExternalTransaction t = provider.map(json("""
                {"id":"usd","description":"Compra exterior","amount":10,"currencyCode":"USD","amountInAccountCurrency":52.30,
                 "type":"DEBIT","date":"2026-03-05T15:30:00.000Z","status":"POSTED"}"""), false);

        assertEquals(0, new BigDecimal("-52.30").compareTo(t.amount()));
    }

    @Test
    void moedaEstrangeiraSemConversaoNaoDeveVirarReais() {
        assertNull(provider.map(json("""
                {"id":"usd","description":"Compra exterior","amount":10,"currencyCode":"USD",
                 "type":"DEBIT","date":"2026-03-05T15:30:00.000Z","status":"POSTED"}"""), false));
    }

    @Test
    void camposNulosDoExemploDaDocumentacaoNaoDevemQuebrar() {
        ExternalTransaction t = provider.map(json("""
                {"id":"6ec156fe","description":"Exemplo TED","descriptionRaw":null,"currencyCode":"BRL","amount":1500,
                 "date":"2021-04-12T00:00:00.000Z","balance":3500,"category":null,"categoryId":null,"providerCode":"123456",
                 "type":"CREDIT","status":"POSTED","paymentData":null,"merchant":null,"providerId":null}"""), false);

        assertEquals("Exemplo TED", t.description());
        assertNull(t.category());
        assertEquals(0, new BigDecimal("1500").compareTo(t.amount()));
    }

    @Test
    void falhaAoCriarOTokenDeveVirarBadGateway() {
        when(client.createConnectToken("u1")).thenThrow(new IllegalStateException("boom"));

        var e = assertThrows(BankIntegrationException.class, () -> provider.createConnectToken("u1"));
        assertEquals(502, e.getStatus().value());
    }
}

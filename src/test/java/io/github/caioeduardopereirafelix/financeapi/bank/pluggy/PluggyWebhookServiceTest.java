package io.github.caioeduardopereirafelix.financeapi.bank.pluggy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.caioeduardopereirafelix.financeapi.bank.BankSyncQueue;
import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.repository.BankConnectionRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PluggyWebhookServiceTest {

    private static final String ITEM = "de7bbf5a-abf2-47e4-94b1-586b36758423";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private BankConnectionRepository connections;
    @Mock
    private TransactionRepository transactions;
    @Mock
    private BankSyncQueue queue;

    private PluggyWebhookService service;
    private BankConnection connection;

    @BeforeEach
    void setUp() {
        service = new PluggyWebhookService(connections, transactions, queue);
        connection = new BankConnection();
        connection.setId(UUID.randomUUID());
    }

    private JsonNode json(String text) {
        try {
            return JSON.readTree(text);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void itemConectado() {
        when(connections.findByProviderAndExternalId("pluggy", ITEM)).thenReturn(Optional.of(connection));
    }

    @Test
    void transacoesCriadasPedemUmaSincronizacao() {
        itemConectado();

        service.handle(json("{\"event\":\"transactions/created\",\"itemId\":\"" + ITEM + "\",\"accountId\":\"a1\"}"));

        verify(queue).enqueue(connection.getId());
    }

    @Test
    void transacoesAtualizadasTambemPedemUmaSincronizacao() {
        itemConectado();

        service.handle(json("{\"event\":\"transactions/updated\",\"itemId\":\"" + ITEM + "\",\"transactionIds\":[\"t1\"]}"));

        verify(queue).enqueue(connection.getId());
        verify(transactions, never()).deleteImported(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void transacoesExcluidasSaoApagadasSoDentroDaConexaoComOPrefixoDoProvedor() {
        itemConectado();

        service.handle(json("{\"event\":\"transactions/deleted\",\"itemId\":\"" + ITEM
                + "\",\"transactionIds\":[\"t1\",\"t2\"]}"));

        ArgumentCaptor<Collection<String>> ids = ArgumentCaptor.forClass(Collection.class);
        verify(transactions).deleteImported(org.mockito.ArgumentMatchers.eq(connection), ids.capture());
        assertEquals(java.util.List.of("pluggy:t1", "pluggy:t2"), java.util.List.copyOf(ids.getValue()));
        verifyNoInteractions(queue);
    }

    @Test
    void muitosIdsSaoApagadosEmLotesDe500() {
        itemConectado();
        String ids = IntStream.range(0, 1200).mapToObj(i -> "\"t" + i + "\"")
                .collect(java.util.stream.Collectors.joining(","));

        service.handle(json("{\"event\":\"transactions/deleted\",\"itemId\":\"" + ITEM + "\",\"transactionIds\":[" + ids + "]}"));

        verify(transactions, times(3)).deleteImported(org.mockito.ArgumentMatchers.eq(connection), any());
    }

    @Test
    void itemQueNaoEstaConectadoAquiEIgnorado() {
        when(connections.findByProviderAndExternalId("pluggy", ITEM)).thenReturn(Optional.empty());

        service.handle(json("{\"event\":\"transactions/deleted\",\"itemId\":\"" + ITEM + "\",\"transactionIds\":[\"t1\"]}"));

        verify(transactions, never()).deleteImported(any(), any());
        verifyNoInteractions(queue);
    }

    @Test
    void itemIdInvalidoNaoChegaNemAoBanco() {
        service.handle(json("{\"event\":\"transactions/created\",\"itemId\":\"../x\"}"));

        verifyNoInteractions(connections, transactions, queue);
    }

    @Test
    void outrosEventosSaoIgnorados() {
        service.handle(json("{\"event\":\"item/updated\",\"itemId\":\"" + ITEM + "\"}"));
        service.handle(json("{}"));

        verifyNoInteractions(connections, transactions, queue);
    }

    @Test
    void excluidasSemListaDeIdsNaoFazNada() {
        itemConectado();

        service.handle(json("{\"event\":\"transactions/deleted\",\"itemId\":\"" + ITEM + "\"}"));

        verify(transactions, never()).deleteImported(any(), any());
    }
}

package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.model.entity.Transaction;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.model.enums.BankConnectionStatus;
import io.github.caioeduardopereirafelix.financeapi.model.enums.CategoryName;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionSource;
import io.github.caioeduardopereirafelix.financeapi.model.enums.TransactionalType;
import io.github.caioeduardopereirafelix.financeapi.repository.BankConnectionRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankSyncServiceTest {

    @Mock
    private BankConnectionRepository connections;
    @Mock
    private TransactionRepository transactions;
    @Mock
    private BankProvider provider;
    @Mock
    private io.github.caioeduardopereirafelix.financeapi.service.CategoryRuleService categoryRules;

    private BankSyncService service;
    private BankConnection connection;
    private User user;

    @BeforeEach
    void setUp() {
        when(provider.name()).thenReturn("test");
        BankProviders providers = new BankProviders(List.of(provider), "test");

        service = new BankSyncService(connections, transactions, providers, new BankCategoryMapper(), categoryRules);

        user = User.builder().id(UUID.randomUUID()).build();
        connection = new BankConnection();
        connection.setId(UUID.randomUUID());
        connection.setUser(user);
        connection.setProvider("test");
        connection.setExternalId("conn-1");
        connection.setStatus(BankConnectionStatus.ACTIVE);

        when(connections.findById(connection.getId())).thenReturn(java.util.Optional.of(connection));
    }

    private ExternalTransaction external(String id, String amount, String category) {
        return new ExternalTransaction(id, "desc " + id, new BigDecimal(amount), Instant.now(), category);
    }

    @Test
    void deveImportarComTipoCategoriaEOrigemCorretos() {
        when(provider.fetchTransactions(anyString(), any())).thenReturn(List.of(external("a", "-40.00", "Groceries")));
        when(transactions.existsByUserAndExternalId(any(), anyString())).thenReturn(false);

        var result = service.syncById(connection.getId());

        assertEquals(1, result.imported());
        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(new BigDecimal("40.00"), saved.getAmount());
        assertEquals(TransactionalType.EXPENSES, saved.getType());
        assertEquals(CategoryName.FOOD, saved.getCategory());
        assertEquals(TransactionSource.BANK, saved.getSource());
        assertEquals(user.getId().toString(), saved.getCreatedBy());
        assertEquals("test:a", saved.getExternalId());
        assertEquals(connection, saved.getBankConnection());
    }

    @Test
    void regraDoUsuarioTemPrioridadeSobreOMapeamentoPadrao() {
        var chave = io.github.caioeduardopereirafelix.financeapi.service.CategoryRuleService
                .lookupKey(TransactionalType.EXPENSES, "uber viagem");
        when(categoryRules.rulesOf(user)).thenReturn(java.util.Map.of(chave, CategoryName.LEISURE));
        when(provider.fetchTransactions(anyString(), any())).thenReturn(List.of(
                new ExternalTransaction("a", "Uber *Viagem 4821", new BigDecimal("-30.00"), Instant.now(), "Transport")));

        service.syncById(connection.getId());

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).save(captor.capture());
        assertEquals(CategoryName.LEISURE, captor.getValue().getCategory());
    }

    @Test
    void regraDeSaidaNaoAtingeEntradaDeMesmaDescricao() {
        var chave = io.github.caioeduardopereirafelix.financeapi.service.CategoryRuleService
                .lookupKey(TransactionalType.EXPENSES, "pix");
        when(categoryRules.rulesOf(user)).thenReturn(java.util.Map.of(chave, CategoryName.LEISURE));
        when(provider.fetchTransactions(anyString(), any())).thenReturn(List.of(
                new ExternalTransaction("a", "Pix", new BigDecimal("50.00"), Instant.now(), null)));

        service.syncById(connection.getId());

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).save(captor.capture());
        assertEquals(CategoryName.OTHER_INCOME, captor.getValue().getCategory());   // nao virou LEISURE
    }

    @Test
    void semRegraUsaOMapeamentoPadrao() {
        when(categoryRules.rulesOf(user)).thenReturn(java.util.Map.of());
        when(provider.fetchTransactions(anyString(), any())).thenReturn(List.of(external("a", "-40.00", "Groceries")));

        service.syncById(connection.getId());

        var captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).save(captor.capture());
        assertEquals(CategoryName.FOOD, captor.getValue().getCategory());
    }

    @Test
    void naoDeveDuplicarTransacaoJaImportada() {
        when(provider.fetchTransactions(anyString(), any())).thenReturn(List.of(external("a", "-40.00", "Groceries")));
        when(transactions.existsByUserAndExternalId(user, "test:a")).thenReturn(true);

        var result = service.syncById(connection.getId());

        assertEquals(0, result.imported());
        assertEquals(1, result.skipped());
        verify(transactions, never()).save(any());
    }

    @Test
    void deveIgnorarValorZeroOuNulo() {
        when(provider.fetchTransactions(anyString(), any())).thenReturn(List.of(
                external("z", "0", null),
                new ExternalTransaction("n", "sem valor", null, Instant.now(), null)));

        var result = service.syncById(connection.getId());

        assertEquals(0, result.imported());
        assertEquals(2, result.skipped());
    }

    @Test
    void deveMarcarErroEPropagarQuandoOProvedorFalha() {
        when(provider.fetchTransactions(anyString(), any())).thenThrow(new RuntimeException("provedor fora do ar"));

        var ex = assertThrows(BankIntegrationException.class, () -> service.syncById(connection.getId()));

        assertEquals(502, ex.getStatus().value());
        assertEquals(BankConnectionStatus.ERROR, connection.getStatus());
        verify(connections).save(connection);
    }

    @Test
    void deveManterAMensagemQueOProvedorDeuEMarcarErro() {
        when(provider.fetchTransactions(anyString(), any())).thenThrow(new BankIntegrationException(
                org.springframework.http.HttpStatus.BAD_GATEWAY, "A Pluggy recusou a operacao (HTTP 403): sem permissao"));

        var ex = assertThrows(BankIntegrationException.class, () -> service.syncById(connection.getId()));

        assertEquals("A Pluggy recusou a operacao (HTTP 403): sem permissao", ex.getMessage());
        assertEquals(BankConnectionStatus.ERROR, connection.getStatus());
    }

    @Test
    void deveRegistrarASincronizacaoEReativarAConexao() {
        connection.setStatus(BankConnectionStatus.ERROR);
        when(provider.fetchTransactions(anyString(), any())).thenReturn(List.of());

        service.syncById(connection.getId());

        assertEquals(BankConnectionStatus.ACTIVE, connection.getStatus());
        assertEquals(true, connection.getLastSyncedAt() != null);
    }

    @Test
    void segundaSincronizacaoRecomecaAntesDaUltimaPorCausaDeLancamentosAtrasados() {
        Instant last = Instant.now().minusSeconds(3600);
        connection.setLastSyncedAt(last);
        when(provider.fetchTransactions(anyString(), any())).thenReturn(List.of());

        service.syncById(connection.getId());

        var since = ArgumentCaptor.forClass(Instant.class);
        verify(provider).fetchTransactions(anyString(), since.capture());
        assertEquals(last.minus(BankSyncService.OVERLAP), since.getValue());
    }
}

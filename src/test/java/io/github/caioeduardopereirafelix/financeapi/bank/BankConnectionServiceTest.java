package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.BankConnectionRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankConnectionServiceTest {

    @Mock
    private BankConnectionRepository connections;
    @Mock
    private TransactionRepository transactions;
    @Mock
    private BankProvider provider;

    private BankConnectionService service;
    private User user;

    @BeforeEach
    void setUp() {
        when(provider.name()).thenReturn("test");
        service = new BankConnectionService(connections, transactions, new BankProviders(List.of(provider), "test"));
        user = User.builder().id(UUID.randomUUID()).build();
    }

    private BankConnection connection(String provider, String externalId) {
        BankConnection c = new BankConnection();
        c.setId(UUID.randomUUID());
        c.setUser(user);
        c.setProvider(provider);
        c.setExternalId(externalId);
        return c;
    }


    @Test
    void tokenDeReautorizacaoTrazOExternalIdParaOWidgetAbrirNaConexao() {
        var c = connection("test", "item-9");
        when(connections.findByIdAndUser(c.getId(), user)).thenReturn(Optional.of(c));
        when(provider.createUpdateToken("item-9", user.getId().toString())).thenReturn("token-upd");

        var reauth = service.createUpdateToken(user, c.getId());

        assertEquals("token-upd", reauth.token());
        assertEquals("test", reauth.provider());
        assertEquals("item-9", reauth.externalId());
    }

    @Test
    void conexaoDeOutroUsuarioNaoGeraTokenDeReautorizacao() {
        var id = UUID.randomUUID();
        when(connections.findByIdAndUser(id, user)).thenReturn(Optional.empty());

        var e = assertThrows(BankIntegrationException.class, () -> service.createUpdateToken(user, id));

        assertEquals(404, e.getStatus().value());
        verify(provider, never()).createUpdateToken(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }


    @Test
    void revokeAllRevogaCadaConexaoDoUsuario() {
        var a = connection("test", "item-a");
        var b = connection("test", "item-b");
        when(connections.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(a, b));

        service.revokeAll(user);

        verify(provider).disconnect("item-a");
        verify(provider).disconnect("item-b");
    }

    @Test
    void falhaDoProvedorInterrompeParaNaoApagarAContaComAutorizacaoViva() {
        var a = connection("test", "item-a");
        var b = connection("test", "item-b");
        when(connections.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(a, b));
        doThrow(new BankIntegrationException(HttpStatus.BAD_GATEWAY, "fora do ar")).when(provider).disconnect("item-a");

        assertThrows(BankIntegrationException.class, () -> service.revokeAll(user));

        verify(provider, never()).disconnect("item-b");
    }

    @Test
    void erroInesperadoDoProvedorVira502() {
        var a = connection("test", "item-a");
        when(connections.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(a));
        doThrow(new IllegalStateException("boom")).when(provider).disconnect("item-a");

        var e = assertThrows(BankIntegrationException.class, () -> service.revokeAll(user));

        assertEquals(502, e.getStatus().value());
    }

    @Test
    void provedorQueNaoEstaConfiguradoNestaInstanciaEPuladoParaNaoTravarAExclusao() {
        var antiga = connection("mock", "demo-1");
        var atual = connection("test", "item-a");
        when(connections.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(antiga, atual));

        service.revokeAll(user);

        verify(provider).disconnect("item-a");
    }
}

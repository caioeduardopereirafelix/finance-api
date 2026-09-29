package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.model.entity.BankConnection;
import io.github.caioeduardopereirafelix.financeapi.model.enums.BankConnectionStatus;
import io.github.caioeduardopereirafelix.financeapi.repository.BankConnectionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankSyncSchedulerTest {

    @Mock
    private BankConnectionRepository connections;
    @Mock
    private BankSyncService syncService;

    private BankConnection connection(BankConnectionStatus status) {
        BankConnection c = new BankConnection();
        c.setId(UUID.randomUUID());
        c.setStatus(status);
        return c;
    }

    @Test
    void deveTentarDeNovoAsConexoesComErroParaQueVoltemAoNormal() {
        var ativa = connection(BankConnectionStatus.ACTIVE);
        var comErro = connection(BankConnectionStatus.ERROR);
        when(connections.findAll()).thenReturn(List.of(ativa, comErro));
        when(syncService.syncById(any())).thenReturn(new BankSyncService.Result(0, 0));

        new BankSyncScheduler(connections, syncService).syncAll();

        verify(syncService).syncById(ativa.getId());
        verify(syncService).syncById(comErro.getId());
    }

    @Test
    void umaConexaoComProblemaNaoImpedeAsOutras() {
        var quebrada = connection(BankConnectionStatus.ERROR);
        var boa = connection(BankConnectionStatus.ACTIVE);
        when(connections.findAll()).thenReturn(List.of(quebrada, boa));
        when(syncService.syncById(quebrada.getId()))
                .thenThrow(new BankIntegrationException(HttpStatus.BAD_GATEWAY, "fora do ar"));
        when(syncService.syncById(boa.getId())).thenReturn(new BankSyncService.Result(1, 0));

        new BankSyncScheduler(connections, syncService).syncAll();

        verify(syncService).syncById(boa.getId());
    }
}

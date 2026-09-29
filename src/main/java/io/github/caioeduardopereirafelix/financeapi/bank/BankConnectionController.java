package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.config.SecurityUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bank")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BankConnectionController {

    private final BankConnectionService connectionService;
    private final BankSyncService syncService;
    private final SecurityUtils securityUtils;

    /** Token para o front abrir o widget de conexao do provedor. */
    @PostMapping("/connect-token")
    public BankConnectionService.ConnectToken connectToken() {
        return connectionService.createConnectToken(securityUtils.getAuthenticatedUser());
    }

    /** Registra a conexao depois que o usuario autorizou o banco no widget. */
    @PostMapping("/connections")
    public ResponseEntity<BankConnectionResponse> connect(@Valid @RequestBody ConnectBankRequest request) {
        var connection = connectionService.connect(securityUtils.getAuthenticatedUser(), request.externalId());
        return ResponseEntity.status(HttpStatus.CREATED).body(BankConnectionResponse.from(connection));
    }

    /** Token para reautorizar uma conexao existente (o banco pediu login de novo). */
    @PostMapping("/connections/{id}/update-token")
    public BankConnectionService.ReauthToken updateToken(@PathVariable UUID id) {
        return connectionService.createUpdateToken(securityUtils.getAuthenticatedUser(), id);
    }

    @GetMapping("/connections")
    public List<BankConnectionResponse> list() {
        return connectionService.list(securityUtils.getAuthenticatedUser()).stream()
                .map(BankConnectionResponse::from)
                .toList();
    }

    @PostMapping("/connections/{id}/sync")
    public BankSyncService.Result sync(@PathVariable UUID id) {
        return syncService.syncForUser(id, securityUtils.getAuthenticatedUser());
    }

    /** Com deleteImported=true apaga tambem as transacoes que vieram dessa conexao. */
    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Void> disconnect(@PathVariable UUID id,
                                           @RequestParam(defaultValue = "false") boolean deleteImported) {
        connectionService.disconnect(securityUtils.getAuthenticatedUser(), id, deleteImported);
        return ResponseEntity.noContent().build();
    }
}

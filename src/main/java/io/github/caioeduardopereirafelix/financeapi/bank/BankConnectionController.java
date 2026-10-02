package io.github.caioeduardopereirafelix.financeapi.bank;

import io.github.caioeduardopereirafelix.financeapi.config.SecurityUtils;
import io.github.caioeduardopereirafelix.financeapi.service.EmailVerificationService;
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
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/connect-token")
    public BankConnectionService.ConnectToken connectToken() {
        var user = securityUtils.getAuthenticatedUser();
        emailVerificationService.requireVerified(user);
        return connectionService.createConnectToken(user);
    }

    @PostMapping("/connections")
    public ResponseEntity<BankConnectionResponse> connect(@Valid @RequestBody ConnectBankRequest request) {
        var user = securityUtils.getAuthenticatedUser();
        emailVerificationService.requireVerified(user);
        var connection = connectionService.connect(user, request.externalId());
        return ResponseEntity.status(HttpStatus.CREATED).body(BankConnectionResponse.from(connection));
    }

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

    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Void> disconnect(@PathVariable UUID id,
                                           @RequestParam(defaultValue = "false") boolean deleteImported) {
        connectionService.disconnect(securityUtils.getAuthenticatedUser(), id, deleteImported);
        return ResponseEntity.noContent().build();
    }
}

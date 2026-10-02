package io.github.caioeduardopereirafelix.financeapi.bank.pluggy;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequiredArgsConstructor
@ConditionalOnExpression("!'${bank.pluggy.webhook-secret:}'.isBlank()")
public class PluggyWebhookController {

    private final PluggyWebhookService service;

    @Value("${bank.pluggy.webhook-secret}")
    private String secret;

    @PostMapping("/webhooks/pluggy/{secret}")
    public ResponseEntity<Void> receive(@PathVariable("secret") String received, @RequestBody JsonNode payload) {
        boolean valid = MessageDigest.isEqual(
                received.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            return ResponseEntity.notFound().build();
        }
        service.handle(payload);
        return ResponseEntity.ok().build();
    }
}

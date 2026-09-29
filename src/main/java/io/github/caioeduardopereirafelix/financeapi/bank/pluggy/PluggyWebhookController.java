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

/**
 * Recebe os webhooks da Pluggy.
 *
 * A rota e publica (a Pluggy nao tem login), entao a autenticidade vem de um segredo no
 * proprio caminho: /webhooks/pluggy/{segredo}. Segredo errado responde 404, como se a rota
 * nao existisse. Sem PLUGGY_WEBHOOK_SECRET a rota nem e criada.
 */
@RestController
@RequiredArgsConstructor
@ConditionalOnExpression("!'${bank.pluggy.webhook-secret:}'.isBlank()")
public class PluggyWebhookController {

    private final PluggyWebhookService service;

    @Value("${bank.pluggy.webhook-secret}")
    private String secret;

    @PostMapping("/webhooks/pluggy/{secret}")
    public ResponseEntity<Void> receive(@PathVariable("secret") String received, @RequestBody JsonNode payload) {
        // comparacao em tempo constante, para o tempo de resposta nao denunciar o segredo
        boolean valid = MessageDigest.isEqual(
                received.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            return ResponseEntity.notFound().build();
        }
        service.handle(payload);
        return ResponseEntity.ok().build();
    }
}

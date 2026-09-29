package io.github.caioeduardopereirafelix.financeapi.bank.pluggy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PluggyConfigTest {

    @Test
    void urlDoWebhookJuntaABaseComOSegredo() {
        assertEquals("https://api.exemplo.com/webhooks/pluggy/abc",
                PluggyConfig.webhookUrl("https://api.exemplo.com", "abc"));
        assertEquals("https://api.exemplo.com/webhooks/pluggy/abc",
                PluggyConfig.webhookUrl("https://api.exemplo.com/", "abc"));
    }

    @Test
    void semBaseOuSemSegredoNaoHaWebhook() {
        assertNull(PluggyConfig.webhookUrl("", "abc"));
        assertNull(PluggyConfig.webhookUrl("https://api.exemplo.com", ""));
        assertNull(PluggyConfig.webhookUrl(null, null));
    }
}

package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.TooManyLoginAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginAttemptServiceTest {

    private AtomicReference<Instant> now;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        now = new AtomicReference<>(Instant.parse("2026-01-01T12:00:00Z"));
        Clock clock = new Clock() {
            @Override public ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };
        service = new LoginAttemptService(3, Duration.ofMinutes(15), clock);
    }

    private void fail(String email, int times) {
        for (int i = 0; i < times; i++) {
            service.checkAllowed(email);
            service.recordFailure(email);
        }
    }

    @Test
    void abaixoDoLimiteNaoTrava() {
        fail("a@b.com", 2);
        assertDoesNotThrow(() -> service.checkAllowed("a@b.com"));
    }

    @Test
    void noLimiteTravaEInformaQuantoEsperar() {
        fail("a@b.com", 3);

        var e = assertThrows(TooManyLoginAttemptsException.class, () -> service.checkAllowed("a@b.com"));

        assertEquals(15 * 60 + 1, e.getRetryAfterSeconds());
        assertEquals("Muitas tentativas de login. Tente novamente em 16 minutos.", e.getMessage());
    }

    @Test
    void travaPorEmailSemDiferenciarMaiusculas() {
        fail("A@B.com", 3);

        assertThrows(TooManyLoginAttemptsException.class, () -> service.checkAllowed("a@b.COM "));
        assertDoesNotThrow(() -> service.checkAllowed("outro@b.com"));
    }

    @Test
    void depoisDoTempoDeTravamentoLibera() {
        fail("a@b.com", 3);
        now.set(now.get().plus(Duration.ofMinutes(15)).plusSeconds(1));

        assertDoesNotThrow(() -> service.checkAllowed("a@b.com"));

        fail("a@b.com", 2);
        assertDoesNotThrow(() -> service.checkAllowed("a@b.com"));
    }

    @Test
    void loginCertoZeraAContagem() {
        fail("a@b.com", 2);
        service.recordSuccess("a@b.com");
        fail("a@b.com", 2);

        assertDoesNotThrow(() -> service.checkAllowed("a@b.com"));
    }

    @Test
    void errosEspalhadosPeloTempoNaoSomam() {
        fail("a@b.com", 2);
        now.set(now.get().plus(Duration.ofMinutes(16)));
        fail("a@b.com", 2);

        assertDoesNotThrow(() -> service.checkAllowed("a@b.com"));
    }
}

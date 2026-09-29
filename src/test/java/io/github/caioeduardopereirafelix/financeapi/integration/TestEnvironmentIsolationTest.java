package io.github.caioeduardopereirafelix.financeapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@ActiveProfiles("test")
class TestEnvironmentIsolationTest {

    @Autowired
    private Environment env;

    @Test
    void oProvedorPadraoDosTestesEOMock() {
        assertEquals("mock", env.getProperty("bank.provider"));
    }

    @Test
    void aPluggyNaoEstaLigadaNosTestes() {
        assertTrue(env.getProperty("bank.pluggy.client-id", "").isBlank());
        assertTrue(env.getProperty("bank.pluggy.client-secret", "").isBlank());
        assertTrue(env.getProperty("bank.pluggy.webhook-secret", "").isBlank());
    }

    @Test
    void nadaDeBancoLigaSozinho() {
        assertFalse(env.getProperty("bank.mock.enabled", Boolean.class, false));
        assertFalse(env.getProperty("bank.sync.enabled", Boolean.class, false));
    }

    @Test
    void oLimiteDeLoginEOPadrao() {
        assertEquals(5, env.getProperty("api.security.login.max-attempts", Integer.class));
    }
}

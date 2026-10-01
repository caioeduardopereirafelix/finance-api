package io.github.caioeduardopereirafelix.financeapi.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecureTokensTest {

    @Test
    void geraTokensDiferentesEUrlSafe() {
        String a = SecureTokens.generate();
        String b = SecureTokens.generate();

        assertNotEquals(a, b);
        assertTrue(a.matches("[A-Za-z0-9_-]{43}"));
    }

    @Test
    void hashEDeterministicoEComSessentaEQuatroCaracteres() {
        String hash = SecureTokens.sha256("abc");

        assertEquals(64, hash.length());
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash);
        assertEquals(hash, SecureTokens.sha256("abc"));
    }
}

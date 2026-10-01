package io.github.caioeduardopereirafelix.financeapi.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailPolicyTest {

    @Test
    void normalizaCaixaEEspacos() {
        assertEquals("caio.felix+teste@gmail.com", EmailPolicy.normalize("  Caio.Felix+Teste@Gmail.COM "));
        assertNull(EmailPolicy.normalize(null));
    }

    @Test
    void aceitaEnderecosComDominioETld() {
        assertTrue("a@b.co".matches(EmailPolicy.PATTERN));
        assertTrue("caio.felix+teste@sub.exemplo.com.br".matches(EmailPolicy.PATTERN));
    }

    @Test
    void recusaEnderecosSemTldOuMalFormados() {
        assertFalse("afafasf@gfsgsg".matches(EmailPolicy.PATTERN));
        assertFalse("a@b.c".matches(EmailPolicy.PATTERN));
        assertFalse("sem-arroba.com".matches(EmailPolicy.PATTERN));
        assertFalse("com espaco@exemplo.com".matches(EmailPolicy.PATTERN));
        assertFalse("dois@@exemplo.com".matches(EmailPolicy.PATTERN));
        assertFalse("@exemplo.com".matches(EmailPolicy.PATTERN));
    }
}

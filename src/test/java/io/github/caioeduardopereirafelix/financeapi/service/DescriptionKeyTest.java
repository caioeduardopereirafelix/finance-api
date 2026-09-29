package io.github.caioeduardopereirafelix.financeapi.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescriptionKeyTest {

    @Test
    void ignoraCaixaAcentoNumeroESimbolo() {
        assertEquals("uber viagem", DescriptionKey.of("Uber *Viagem 1234"));
        assertEquals("uber viagem", DescriptionKey.of("UBER  *VIAGEM   99"));
    }

    @Test
    void comprasDoMesmoLugarComCodigosDiferentesTemAMesmaChave() {
        assertEquals(DescriptionKey.of("PAG*Padaria Pao Quente 0042"), DescriptionKey.of("Pag*Padaria Pão Quente 7781"));
    }

    @Test
    void lugaresDiferentesTemChavesDiferentes() {
        assertEquals("false", String.valueOf(DescriptionKey.of("Supermercado Central")
                .equals(DescriptionKey.of("Supermercado Norte"))));
    }

    @Test
    void usaSoAsPrimeirasQuatroPalavras() {
        assertEquals("compra no cartao debito", DescriptionKey.of("Compra no cartao debito loja x filial 3"));
    }

    @Test
    void palavrasDeUmaLetraSaoDescartadas() {
        assertEquals("uber viagem", DescriptionKey.of("Uber a Viagem"));
    }

    @Test
    void semLetrasNaoHaChave() {
        assertEquals("", DescriptionKey.of("12345 *** 99"));
        assertEquals("", DescriptionKey.of("   "));
        assertEquals("", DescriptionKey.of(null));
    }
}

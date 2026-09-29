package io.github.caioeduardopereirafelix.financeapi.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * O que identifica "o mesmo estabelecimento" numa descricao de extrato.
 *
 * Descricoes do banco trazem codigos e numeros que mudam a cada compra ("UBER *TRIP 9F3K",
 * "Uber *Viagem 1234"). A chave usa so as primeiras palavras, em minusculas, sem acento, numero
 * ou simbolo: o bastante para juntar as compras do mesmo lugar sem juntar coisas diferentes.
 * Vazia quando a descricao nao tem letras suficientes para identificar nada.
 */
public final class DescriptionKey {

    static final int MAX_WORDS = 4;

    private DescriptionKey() {
    }

    public static String of(String description) {
        if (description == null) {
            return "";
        }
        String plain = Normalizer.normalize(description, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}]+", " ")
                .trim();

        return Arrays.stream(plain.split(" "))
                .filter(word -> word.length() >= 2)
                .limit(MAX_WORDS)
                .collect(Collectors.joining(" "));
    }
}

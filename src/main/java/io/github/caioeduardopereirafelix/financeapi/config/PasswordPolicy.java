package io.github.caioeduardopereirafelix.financeapi.config;

/**
 * Regra unica de senha, usada por todos os DTOs que criam ou trocam senha.
 *
 * O login nao aplica o tamanho minimo: quem cadastrou uma senha mais curta
 * (quando o minimo era menor) precisa continuar conseguindo entrar.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 100;

    private PasswordPolicy() {
    }
}

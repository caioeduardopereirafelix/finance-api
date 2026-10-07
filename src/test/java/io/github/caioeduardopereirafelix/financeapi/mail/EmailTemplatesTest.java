package io.github.caioeduardopereirafelix.financeapi.mail;

import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplatesTest {

    private final EmailTemplates templates = new EmailTemplates("https://app.exemplo.com//");
    private final User user = User.builder().name("Caio").email("caio@test.com").build();

    @Test
    void confirmacaoDeEmailTrazOTokenNoFragmentoEAValidadeEmHoras() {
        EmailMessage message = templates.emailVerification(user, 24, "abc");

        assertEquals("Confirme seu e-mail", message.subject());
        assertTrue(message.body().contains("Olá, Caio."));
        assertTrue(message.body().contains("24 horas"));
        assertTrue(message.body().lines().anyMatch("https://app.exemplo.com/confirmar-email#token=abc"::equals));
    }

    @Test
    void redefinicaoDeSenhaTrazOTokenNoFragmentoEAValidadeEmMinutos() {
        EmailMessage message = templates.passwordReset(user, 30, "xyz");

        assertEquals("Redefinição de senha", message.subject());
        assertTrue(message.body().contains("30 minutos"));
        assertTrue(message.body().lines().anyMatch("https://app.exemplo.com/redefinir-senha#token=xyz"::equals));
    }

    @Test
    void avisoDeSenhaAlteradaNaoTrazLink() {
        EmailMessage message = templates.passwordChanged(user);

        assertEquals("Sua senha foi alterada", message.subject());
        assertFalse(message.body().contains("http"));
    }

    @Test
    void avisoDeCadastroComEmailExistenteApontaParaEsqueciSenha() {
        EmailMessage message = templates.registrationAttemptOnExistingEmail(user);

        assertTrue(message.body().contains("https://app.exemplo.com/esqueci-senha"));
    }
}

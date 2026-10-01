package io.github.caioeduardopereirafelix.financeapi.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailSenderTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
    private final SmtpEmailSender sender = new SmtpEmailSender(mailSender);

    SmtpEmailSenderTest() {
        ReflectionTestUtils.setField(sender, "from", "no-reply@finance.test");
        when(mailSender.createMimeMessage()).thenReturn(message);
    }

    @Test
    void montaEEnviaAMensagemEmTextoPuroUtf8() throws Exception {
        sender.send("caio@test.com", "Redefinição de senha", "Olá, Caio.");

        verify(mailSender).send(message);
        assertEquals("no-reply@finance.test", message.getFrom()[0].toString());
        assertEquals("caio@test.com", message.getAllRecipients()[0].toString());
        assertEquals("Redefinição de senha", message.getSubject());
        assertTrue(message.getContentType().startsWith("text/plain"));
    }

    @Test
    void falhaDoServidorViraIllegalStateException() {
        doThrow(new org.springframework.mail.MailSendException("fora do ar")).when(mailSender).send(any(MimeMessage.class));

        assertThrows(IllegalStateException.class, () -> sender.send("caio@test.com", "a", "b"));
    }
}

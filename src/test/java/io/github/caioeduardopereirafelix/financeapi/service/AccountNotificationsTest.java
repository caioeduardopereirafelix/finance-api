package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailTemplates;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountNotificationsTest {

    @Mock
    private EmailDispatcher emailDispatcher;

    @Spy
    private EmailTemplates templates = new EmailTemplates("https://app.exemplo.com/");

    @InjectMocks
    private AccountNotifications notifications;

    private User user(String email) {
        return User.builder().name("Caio").email(email).build();
    }

    @Test
    void avisaODonoDoEmailComOLinkDeRecuperarASenha() {
        notifications.registrationAttemptOnExistingEmail(user("caio@test.com"));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailDispatcher).dispatch(eq("caio@test.com"), anyString(), body.capture());
        assertTrue(body.getValue().contains("https://app.exemplo.com/esqueci-senha"));
    }

    @Test
    void naoMandaOutroAvisoParaOMesmoEmailDentroDeUmaHora() {
        notifications.registrationAttemptOnExistingEmail(user("caio@test.com"));
        notifications.registrationAttemptOnExistingEmail(user("Caio@Test.com"));
        notifications.registrationAttemptOnExistingEmail(user("caio@test.com"));

        verify(emailDispatcher, times(1)).dispatch(eq("caio@test.com"), anyString(), anyString());
    }

    @Test
    void emailsDiferentesRecebemCadaUmOSeuAviso() {
        notifications.registrationAttemptOnExistingEmail(user("a@test.com"));
        notifications.registrationAttemptOnExistingEmail(user("b@test.com"));

        verify(emailDispatcher).dispatch(eq("a@test.com"), anyString(), anyString());
        verify(emailDispatcher).dispatch(eq("b@test.com"), anyString(), anyString());
    }
}

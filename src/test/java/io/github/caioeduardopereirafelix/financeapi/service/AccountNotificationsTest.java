package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountNotificationsTest {

    @Mock
    private EmailDispatcher emailDispatcher;

    @InjectMocks
    private AccountNotifications notifications;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notifications, "frontendUrl", "https://app.exemplo.com/");
    }

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

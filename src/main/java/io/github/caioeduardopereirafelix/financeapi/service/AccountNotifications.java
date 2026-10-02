package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AccountNotifications {

    static final int MAX_TRACKED = 10_000;
    static final Duration REGISTRATION_NOTICE_INTERVAL = Duration.ofHours(1);

    private final EmailDispatcher emailDispatcher;
    private final Map<String, Instant> lastRegistrationNotice = new ConcurrentHashMap<>();

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public void passwordChanged(User user) {
        emailDispatcher.dispatch(user.getEmail(),
                "Sua senha foi alterada",
                """
                Olá, %s.

                A senha da sua conta no Finance acabou de ser alterada. Por segurança, nenhum dispositivo consegue mais renovar o acesso com a senha antiga.

                Se foi você, não precisa fazer nada. Se não foi, peça uma nova redefinição de senha agora mesmo.
                """.formatted(user.getName()));
    }

    public void registrationAttemptOnExistingEmail(User user) {
        Instant now = Instant.now();
        String key = user.getEmail().toLowerCase(Locale.ROOT);

        if (lastRegistrationNotice.size() >= MAX_TRACKED) {
            lastRegistrationNotice.values().removeIf(at -> at.plus(REGISTRATION_NOTICE_INTERVAL).isBefore(now));
        }
        Instant last = lastRegistrationNotice.get(key);
        if (last != null && last.plus(REGISTRATION_NOTICE_INTERVAL).isAfter(now)) {
            return;
        }
        if (lastRegistrationNotice.size() < MAX_TRACKED) {
            lastRegistrationNotice.put(key, now);
        }

        String link = frontendUrl.replaceAll("/+$", "") + "/esqueci-senha";
        emailDispatcher.dispatch(user.getEmail(),
                "Alguém tentou criar uma conta com o seu e-mail",
                """
                Olá, %s.

                Alguém tentou criar uma nova conta no Finance usando este e-mail, mas ele já tem uma conta. Nada foi alterado.

                Se foi você e você não lembra da senha, redefina por aqui:

                %s

                Se não foi você, pode ignorar este e-mail.
                """.formatted(user.getName(), link));
    }
}

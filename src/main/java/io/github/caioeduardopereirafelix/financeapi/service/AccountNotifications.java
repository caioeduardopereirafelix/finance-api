package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailMessage;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailTemplates;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import lombok.RequiredArgsConstructor;
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
    private final EmailTemplates templates;
    private final Map<String, Instant> lastRegistrationNotice = new ConcurrentHashMap<>();

    public void passwordChanged(User user) {
        send(user, templates.passwordChanged(user));
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

        send(user, templates.registrationAttemptOnExistingEmail(user));
    }

    private void send(User user, EmailMessage message) {
        emailDispatcher.dispatch(user.getEmail(), message.subject(), message.body());
    }
}

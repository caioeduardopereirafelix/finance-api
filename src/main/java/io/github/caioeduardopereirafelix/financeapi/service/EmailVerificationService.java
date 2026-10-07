package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.EmailNotVerifiedException;
import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidEmailVerificationToken;
import io.github.caioeduardopereirafelix.financeapi.exceptions.VerificationResendTooSoonException;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailMessage;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailTemplates;
import io.github.caioeduardopereirafelix.financeapi.model.entity.EmailVerificationToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.EmailVerificationTokenRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.OneTimeTokenRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmailVerificationService extends OneTimeTokenService<EmailVerificationToken> {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailDispatcher emailDispatcher;
    private final EmailTemplates templates;

    @Value("${api.security.email-verification.expiration-hours:24}")
    private long expirationHours;

    @Value("${api.security.email-verification.cooldown-seconds:60}")
    private long cooldownSeconds;

    public void sendInitial(User user) {
        issueAndSend(user);
    }

    public void resend(User user) {
        if (user.getEmailVerifiedAt() != null) {
            return;
        }
        cooldownLeft(user, Duration.ofSeconds(cooldownSeconds)).ifPresent(left -> {
            throw new VerificationResendTooSoonException(left.toSeconds() + 1);
        });
        issueAndSend(user);
    }

    @Transactional
    public void verify(String token) {
        User user = redeem(token);
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
            userRepository.save(user);
        }
        tokenRepository.deleteByUser(user);
    }

    public void requireVerified(User user) {
        if (user.getEmailVerifiedAt() == null) {
            throw new EmailNotVerifiedException();
        }
    }

    private void issueAndSend(User user) {
        String token = issue(user, Duration.ofHours(expirationHours));
        EmailMessage message = templates.emailVerification(user, expirationHours, token);
        emailDispatcher.dispatch(user.getEmail(), message.subject(), message.body());
    }

    @Override
    protected OneTimeTokenRepository<EmailVerificationToken> tokens() {
        return tokenRepository;
    }

    @Override
    protected EmailVerificationToken newToken() {
        return new EmailVerificationToken();
    }

    @Override
    protected RuntimeException invalidToken() {
        return new InvalidEmailVerificationToken("Invalid or expired email verification link");
    }
}

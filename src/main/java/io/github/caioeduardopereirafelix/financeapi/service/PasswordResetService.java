package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.config.EmailPolicy;
import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidTokenException;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailMessage;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailTemplates;
import io.github.caioeduardopereirafelix.financeapi.model.entity.PasswordResetToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.OneTimeTokenRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.PasswordResetTokenRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PasswordResetService extends OneTimeTokenService<PasswordResetToken> {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttempts;
    private final EmailDispatcher emailDispatcher;
    private final AccountNotifications accountNotifications;
    private final EmailTemplates templates;

    @Value("${api.security.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${api.security.password-reset.cooldown-seconds:60}")
    private long cooldownSeconds;

    public void requestReset(String email) {
        userRepository.findByEmail(EmailPolicy.normalize(email)).ifPresent(this::issueAndSend);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = redeem(token);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.deleteByUser(user);
        refreshTokenService.revokeAllFor(user);
        loginAttempts.recordSuccess(user.getEmail());

        afterCommit(() -> accountNotifications.passwordChanged(user));
    }

    private void issueAndSend(User user) {
        if (cooldownLeft(user, Duration.ofSeconds(cooldownSeconds)).isPresent()) {
            return;
        }
        String token = issue(user, Duration.ofMinutes(expirationMinutes));
        EmailMessage message = templates.passwordReset(user, expirationMinutes, token);
        emailDispatcher.dispatch(user.getEmail(), message.subject(), message.body());
    }

    @Override
    protected OneTimeTokenRepository<PasswordResetToken> tokens() {
        return tokenRepository;
    }

    @Override
    protected PasswordResetToken newToken() {
        return new PasswordResetToken();
    }

    @Override
    protected RuntimeException invalidToken() {
        return new InvalidTokenException("Invalid or expired password reset link");
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }
}

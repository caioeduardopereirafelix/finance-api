package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.config.EmailPolicy;
import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidPasswordResetToken;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.model.entity.PasswordResetToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.PasswordResetTokenRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttempts;
    private final EmailDispatcher emailDispatcher;
    private final AccountNotifications accountNotifications;

    @Value("${api.security.password-reset.expiration-minutes:30}")
    private long expirationMinutes;

    @Value("${api.security.password-reset.cooldown-seconds:60}")
    private long cooldownSeconds;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public void requestReset(String email) {
        userRepository.findByEmail(EmailPolicy.normalize(email)).ifPresent(this::issueToken);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken stored = tokenRepository.findByTokenHash(SecureTokens.sha256(token))
                .orElseThrow(PasswordResetService::invalid);

        if (stored.getExpiresAt().isBefore(Instant.now()) || tokenRepository.consume(stored.getId()) == 0) {
            throw invalid();
        }

        User user = stored.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepository.deleteByUser(user);
        refreshTokenService.revokeAllFor(user);
        loginAttempts.recordSuccess(user.getEmail());

        afterCommit(() -> accountNotifications.passwordChanged(user));
    }

    private void issueToken(User user) {
        Instant now = Instant.now();

        tokenRepository.deleteExpiredBefore(now);
        if (tokenRepository.existsByUserAndCreatedAtAfter(user, now.minusSeconds(cooldownSeconds))) {
            return;
        }
        tokenRepository.deleteByUser(user);

        String token = SecureTokens.generate();
        tokenRepository.save(PasswordResetToken.builder()
                .tokenHash(SecureTokens.sha256(token))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusSeconds(expirationMinutes * 60))
                .build());

        String link = frontendUrl.replaceAll("/+$", "") + "/redefinir-senha#token=" + token;

        emailDispatcher.dispatch(user.getEmail(),
                "Redefinição de senha",
                """
                Olá, %s.

                Recebemos um pedido para redefinir a senha da sua conta no Finance. Para escolher uma nova senha, abra o link abaixo. Ele vale por %d minutos e só pode ser usado uma vez:

                %s

                Se você não pediu isso, ignore este e-mail: sua senha continua a mesma.
                """.formatted(user.getName(), expirationMinutes, link));
    }

    private static InvalidPasswordResetToken invalid() {
        return new InvalidPasswordResetToken("Invalid or expired password reset link");
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

package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.exceptions.EmailNotVerifiedException;
import io.github.caioeduardopereirafelix.financeapi.exceptions.InvalidEmailVerificationToken;
import io.github.caioeduardopereirafelix.financeapi.exceptions.VerificationResendTooSoonException;
import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.model.entity.EmailVerificationToken;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import io.github.caioeduardopereirafelix.financeapi.repository.EmailVerificationTokenRepository;
import io.github.caioeduardopereirafelix.financeapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailDispatcher emailDispatcher;

    @Value("${api.security.email-verification.expiration-hours:24}")
    private long expirationHours;

    @Value("${api.security.email-verification.cooldown-seconds:60}")
    private long cooldownSeconds;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public void sendInitial(User user) {
        issueAndSend(user);
    }

    public void resend(User user) {
        if (user.getEmailVerifiedAt() != null) {
            return;
        }
        tokenRepository.findFirstByUserOrderByCreatedAtDesc(user).ifPresent(latest -> {
            Instant releasedAt = latest.getCreatedAt().plusSeconds(cooldownSeconds);
            Instant now = Instant.now();
            if (now.isBefore(releasedAt)) {
                throw new VerificationResendTooSoonException(Duration.between(now, releasedAt).toSeconds() + 1);
            }
        });
        issueAndSend(user);
    }

    @Transactional
    public void verify(String token) {
        EmailVerificationToken stored = tokenRepository.findByTokenHash(SecureTokens.sha256(token))
                .orElseThrow(EmailVerificationService::invalid);

        if (stored.getExpiresAt().isBefore(Instant.now()) || tokenRepository.consume(stored.getId()) == 0) {
            throw invalid();
        }

        User user = stored.getUser();
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
        Instant now = Instant.now();

        tokenRepository.deleteExpiredBefore(now);
        tokenRepository.deleteByUser(user);

        String token = SecureTokens.generate();
        tokenRepository.save(EmailVerificationToken.builder()
                .tokenHash(SecureTokens.sha256(token))
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusSeconds(expirationHours * 3600))
                .build());

        String link = frontendUrl.replaceAll("/+$", "") + "/confirmar-email#token=" + token;

        emailDispatcher.dispatch(user.getEmail(),
                "Confirme seu e-mail",
                """
                Olá, %s.

                Para confirmar seu e-mail e liberar a conexão com seus bancos no Finance, abra o link abaixo. Ele vale por %d horas e só pode ser usado uma vez:

                %s

                Se você não criou uma conta no Finance, ignore este e-mail.
                """.formatted(user.getName(), expirationHours, link));
    }

    private static InvalidEmailVerificationToken invalid() {
        return new InvalidEmailVerificationToken("Invalid or expired email verification link");
    }
}

package io.github.caioeduardopereirafelix.financeapi.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "mail.transport", havingValue = "log", matchIfMissing = true)
public class LogEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("E-mail (transporte log, nada foi enviado)\nPara: {}\nAssunto: {}\n\n{}", to, subject, body);
    }
}

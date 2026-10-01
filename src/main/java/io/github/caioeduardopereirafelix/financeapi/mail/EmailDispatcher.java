package io.github.caioeduardopereirafelix.financeapi.mail;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class EmailDispatcher {

    private final EmailSender sender;
    private final ExecutorService executor;

    public EmailDispatcher(EmailSender sender, @Value("${mail.async:true}") boolean async) {
        this.sender = sender;
        this.executor = async ? Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "email-dispatcher");
            thread.setDaemon(true);
            return thread;
        }) : null;
    }

    public void dispatch(String to, String subject, String body) {
        if (executor == null) {
            deliver(to, subject, body);
            return;
        }
        executor.execute(() -> deliver(to, subject, body));
    }

    private void deliver(String to, String subject, String body) {
        try {
            sender.send(to, subject, body);
        } catch (RuntimeException e) {
            log.error("Falha ao enviar e-mail '{}'", subject, e);
        }
    }
}

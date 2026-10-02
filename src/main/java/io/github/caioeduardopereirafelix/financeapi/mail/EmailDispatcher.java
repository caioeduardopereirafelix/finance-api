package io.github.caioeduardopereirafelix.financeapi.mail;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class EmailDispatcher {

    private static final String METRIC = "finance.mail.sent";

    private final EmailSender sender;
    private final ScheduledExecutorService executor;
    private final int maxAttempts;
    private final long backoffMillis;
    private final MeterRegistry metrics;

    @Autowired
    public EmailDispatcher(EmailSender sender,
                           @Value("${mail.async:true}") boolean async,
                           @Value("${mail.max-attempts:3}") int maxAttempts,
                           @Value("${mail.retry-backoff-seconds:2}") long backoffSeconds,
                           MeterRegistry metrics) {
        this(sender, async, maxAttempts, Duration.ofSeconds(backoffSeconds), metrics);
    }

    EmailDispatcher(EmailSender sender, boolean async, int maxAttempts, Duration backoff, MeterRegistry metrics) {
        this.sender = sender;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoffMillis = Math.max(0, backoff.toMillis());
        this.metrics = metrics;
        this.executor = async ? Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "email-dispatcher");
            thread.setDaemon(true);
            return thread;
        }) : null;
    }

    public void dispatch(String to, String subject, String body) {
        if (executor == null) {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                if (attempt(to, subject, body, attempt)) {
                    return;
                }
            }
            return;
        }
        executor.execute(() -> scheduled(to, subject, body, 1));
    }

    private void scheduled(String to, String subject, String body, int attempt) {
        if (attempt(to, subject, body, attempt)) {
            return;
        }
        if (attempt < maxAttempts) {
            long delay = backoffMillis * (long) Math.pow(5, attempt - 1);
            executor.schedule(() -> scheduled(to, subject, body, attempt + 1), delay, TimeUnit.MILLISECONDS);
        }
    }

    private boolean attempt(String to, String subject, String body, int attempt) {
        try {
            sender.send(to, subject, body);
            metrics.counter(METRIC, "result", "ok").increment();
            log.info("E-mail '{}' entregue ao provedor", subject);
            return true;
        } catch (RuntimeException e) {
            if (attempt < maxAttempts) {
                log.warn("Falha ao enviar e-mail '{}' (tentativa {} de {}): {}", subject, attempt, maxAttempts, e.getMessage());
            } else {
                metrics.counter(METRIC, "result", "failed").increment();
                log.error("Falha ao enviar e-mail '{}' depois de {} tentativas", subject, maxAttempts, e);
            }
            return false;
        }
    }
}

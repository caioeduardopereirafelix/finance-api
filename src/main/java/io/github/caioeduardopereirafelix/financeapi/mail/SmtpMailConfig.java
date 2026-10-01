package io.github.caioeduardopereirafelix.financeapi.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@ConditionalOnProperty(name = "mail.transport", havingValue = "smtp")
public class SmtpMailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${mail.smtp.host:}") String host,
            @Value("${mail.smtp.port:587}") int port,
            @Value("${mail.smtp.username:}") String username,
            @Value("${mail.smtp.password:}") String password,
            @Value("${mail.smtp.starttls:true}") boolean starttls) {

        if (host.isBlank()) {
            throw new IllegalStateException("MAIL_HOST e obrigatorio quando MAIL_TRANSPORT=smtp");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));

        if (!username.isBlank()) {
            sender.setUsername(username);
            sender.setPassword(password);
            props.put("mail.smtp.auth", "true");
        }
        return sender;
    }
}

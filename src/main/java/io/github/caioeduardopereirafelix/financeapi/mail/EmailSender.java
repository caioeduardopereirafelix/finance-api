package io.github.caioeduardopereirafelix.financeapi.mail;

public interface EmailSender {

    void send(String to, String subject, String body);
}

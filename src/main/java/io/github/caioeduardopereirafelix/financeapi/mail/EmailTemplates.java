package io.github.caioeduardopereirafelix.financeapi.mail;

import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplates {

    private final String frontendUrl;

    public EmailTemplates(@Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public EmailMessage emailVerification(User user, long validForHours, String token) {
        String link = frontendUrl + "/confirmar-email#token=" + token;
        return new EmailMessage("Confirme seu e-mail", """
                Olá, %s.

                Para confirmar seu e-mail e liberar a conexão com seus bancos no Finance, abra o link abaixo. Ele vale por %d horas e só pode ser usado uma vez:

                %s

                Se você não criou uma conta no Finance, ignore este e-mail.
                """.formatted(user.getName(), validForHours, link));
    }

    public EmailMessage passwordReset(User user, long validForMinutes, String token) {
        String link = frontendUrl + "/redefinir-senha#token=" + token;
        return new EmailMessage("Redefinição de senha", """
                Olá, %s.

                Recebemos um pedido para redefinir a senha da sua conta no Finance. Para escolher uma nova senha, abra o link abaixo. Ele vale por %d minutos e só pode ser usado uma vez:

                %s

                Se você não pediu isso, ignore este e-mail: sua senha continua a mesma.
                """.formatted(user.getName(), validForMinutes, link));
    }

    public EmailMessage passwordChanged(User user) {
        return new EmailMessage("Sua senha foi alterada", """
                Olá, %s.

                A senha da sua conta no Finance acabou de ser alterada. Por segurança, nenhum dispositivo consegue mais renovar o acesso com a senha antiga.

                Se foi você, não precisa fazer nada. Se não foi, peça uma nova redefinição de senha agora mesmo.
                """.formatted(user.getName()));
    }

    public EmailMessage registrationAttemptOnExistingEmail(User user) {
        String link = frontendUrl + "/esqueci-senha";
        return new EmailMessage("Alguém tentou criar uma conta com o seu e-mail", """
                Olá, %s.

                Alguém tentou criar uma nova conta no Finance usando este e-mail, mas ele já tem uma conta. Nada foi alterado.

                Se foi você e você não lembra da senha, redefina por aqui:

                %s

                Se não foi você, pode ignorar este e-mail.
                """.formatted(user.getName(), link));
    }
}

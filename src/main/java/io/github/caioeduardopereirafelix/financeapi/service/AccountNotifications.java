package io.github.caioeduardopereirafelix.financeapi.service;

import io.github.caioeduardopereirafelix.financeapi.mail.EmailDispatcher;
import io.github.caioeduardopereirafelix.financeapi.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountNotifications {

    private final EmailDispatcher emailDispatcher;

    public void passwordChanged(User user) {
        emailDispatcher.dispatch(user.getEmail(),
                "Sua senha foi alterada",
                """
                Olá, %s.

                A senha da sua conta no Finance acabou de ser alterada. Por segurança, nenhum dispositivo consegue mais renovar o acesso com a senha antiga.

                Se foi você, não precisa fazer nada. Se não foi, peça uma nova redefinição de senha agora mesmo.
                """.formatted(user.getName()));
    }
}

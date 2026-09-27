package com.example.arbomachine.domain.user.service.passwordresetfacade;

import org.springframework.stereotype.Component;

/** Rzuca wyjatek, jesli token nie istnieje albo wygasl - frontend sprawdza link przed pokazaniem formularza. */
@Component
public class EnsurePasswordResetTokenIsValidHandler {
    private final FindValidPasswordResetTokenHandler findValidPasswordResetTokenHandler;

    public EnsurePasswordResetTokenIsValidHandler(FindValidPasswordResetTokenHandler findValidPasswordResetTokenHandler) {
        this.findValidPasswordResetTokenHandler = findValidPasswordResetTokenHandler;
    }

    public void handle(String resetToken) {
        findValidPasswordResetTokenHandler.handle(resetToken);
    }
}

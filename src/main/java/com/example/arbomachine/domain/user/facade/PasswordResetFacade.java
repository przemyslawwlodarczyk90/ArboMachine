package com.example.arbomachine.domain.user.facade;

import com.example.arbomachine.domain.user.service.passwordresetfacade.EnsurePasswordResetTokenIsValidHandler;
import com.example.arbomachine.domain.user.service.passwordresetfacade.ResetPasswordUsingTokenHandler;
import com.example.arbomachine.domain.user.service.passwordresetfacade.SendPasswordResetLinkIfAccountExistsHandler;
import org.springframework.stereotype.Component;

/** Reset zapomnianego hasla: link mailem, sprawdzenie tokenu, nowe haslo. Deleguje do service/passwordresetfacade. */
@Component
public class PasswordResetFacade {
    private final SendPasswordResetLinkIfAccountExistsHandler sendPasswordResetLinkIfAccountExistsHandler;
    private final EnsurePasswordResetTokenIsValidHandler ensurePasswordResetTokenIsValidHandler;
    private final ResetPasswordUsingTokenHandler resetPasswordUsingTokenHandler;

    public PasswordResetFacade(SendPasswordResetLinkIfAccountExistsHandler sendPasswordResetLinkIfAccountExistsHandler,
                               EnsurePasswordResetTokenIsValidHandler ensurePasswordResetTokenIsValidHandler,
                               ResetPasswordUsingTokenHandler resetPasswordUsingTokenHandler) {
        this.sendPasswordResetLinkIfAccountExistsHandler = sendPasswordResetLinkIfAccountExistsHandler;
        this.ensurePasswordResetTokenIsValidHandler = ensurePasswordResetTokenIsValidHandler;
        this.resetPasswordUsingTokenHandler = resetPasswordUsingTokenHandler;
    }

    public void sendPasswordResetLinkIfAccountExists(String email) {
        sendPasswordResetLinkIfAccountExistsHandler.handle(email);
    }

    public void ensurePasswordResetTokenIsValid(String resetToken) {
        ensurePasswordResetTokenIsValidHandler.handle(resetToken);
    }

    public void resetPasswordUsingToken(String resetToken, String newPlainTextPassword) {
        resetPasswordUsingTokenHandler.handle(resetToken, newPlainTextPassword);
    }
}

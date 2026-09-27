package com.example.arbomachine.domain.user.service.passwordresetfacade;

import com.example.arbomachine.domain.user.entity.PasswordResetToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Ustawia nowe haslo i kasuje token - link dziala tylko raz. */
@Component
public class ResetPasswordUsingTokenHandler {
    private final FindValidPasswordResetTokenHandler findValidPasswordResetTokenHandler;
    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public ResetPasswordUsingTokenHandler(FindValidPasswordResetTokenHandler findValidPasswordResetTokenHandler,
                                          UserAccountRepository userAccountRepository,
                                          PasswordResetTokenRepository passwordResetTokenRepository,
                                          PasswordEncoder passwordEncoder) {
        this.findValidPasswordResetTokenHandler = findValidPasswordResetTokenHandler;
        this.userAccountRepository = userAccountRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void handle(String resetToken, String newPlainTextPassword) {
        PasswordResetToken passwordResetToken = findValidPasswordResetTokenHandler.handle(resetToken);

        UserAccount userAccount = passwordResetToken.getUserAccount();
        userAccount.setPasswordHash(passwordEncoder.encode(newPlainTextPassword));
        userAccountRepository.save(userAccount);

        passwordResetTokenRepository.delete(passwordResetToken);
    }
}

package com.example.arbomachine.domain.user.service.passwordresetfacade;

import com.example.arbomachine.domain.user.entity.PasswordResetToken;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.service.EmailSenderService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Wysyla link do resetu hasla. Dla nieznanego, niepotwierdzonego albo zablokowanego adresu po cichu nic nie robi -
 * endpoint nie moze zdradzac, czy konto istnieje. Poprzednie, niewykorzystane linki danego konta przestaja dzialac.
 */
@Component
public class SendPasswordResetLinkIfAccountExistsHandler {
    private static final String PASSWORD_RESET_EMAIL_SUBJECT = "Reset your password";

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailSenderService emailSenderService;

    // Inicjalizatory pol dzialaja jako wartosci domyslne w testach jednostkowych (tam Spring nie wstrzykuje @Value).
    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl = "http://localhost:5173";

    @Value("${app.password-reset.link-validity-minutes:30}")
    private long resetLinkValidityMinutes = 30;

    public SendPasswordResetLinkIfAccountExistsHandler(UserAccountRepository userAccountRepository,
                                                       PasswordResetTokenRepository passwordResetTokenRepository,
                                                       EmailSenderService emailSenderService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailSenderService = emailSenderService;
    }

    @Transactional
    public void handle(String email) {
        userAccountRepository.findByEmailAndEmailConfirmed(email, true)
                .filter(userAccount -> !userAccount.isLockedByAdmin())
                .ifPresent(userAccount -> {
                    passwordResetTokenRepository.deleteByUserAccount(userAccount);

                    String resetToken = UUID.randomUUID().toString();
                    passwordResetTokenRepository.save(PasswordResetToken.builder()
                            .token(resetToken)
                            .createdAt(LocalDateTime.now())
                            .expiresAt(LocalDateTime.now().plusMinutes(resetLinkValidityMinutes))
                            .userAccount(userAccount)
                            .build());

                    String resetLink = frontendBaseUrl + "/reset-password?token=" + resetToken;
                    emailSenderService.sendPasswordResetEmail(userAccount.getEmail(), PASSWORD_RESET_EMAIL_SUBJECT,
                            userAccount.getFirstName(), resetLink, resetLinkValidityMinutes);
                });
    }
}

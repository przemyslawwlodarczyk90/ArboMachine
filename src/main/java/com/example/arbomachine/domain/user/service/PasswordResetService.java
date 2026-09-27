package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.entity.PasswordResetToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.TokenExpiredException;
import com.example.arbomachine.domain.user.exception.TokenNotFoundException;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.service.EmailSenderService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/** Reset zapomnianego hasla: wysylka linku, sprawdzenie tokenu i ustawienie nowego hasla. */
@Service
public class PasswordResetService {
    private static final String PASSWORD_RESET_EMAIL_SUBJECT = "Reset your password";

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSenderService emailSenderService;

    // Inicjalizatory pol dzialaja jako wartosci domyslne w testach jednostkowych (tam Spring nie wstrzykuje @Value).
    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl = "http://localhost:5173";

    @Value("${app.password-reset.link-validity-minutes:30}")
    private long resetLinkValidityMinutes = 30;

    public PasswordResetService(UserAccountRepository userAccountRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailSenderService emailSenderService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSenderService = emailSenderService;
    }

    /**
     * Wysyla link do resetu hasla. Dla nieznanego, niepotwierdzonego albo zablokowanego adresu
     * po cichu nic nie robi - endpoint nie moze zdradzac, czy konto istnieje.
     * Poprzednie, niewykorzystane linki danego konta przestaja dzialac.
     */
    @Transactional
    public void sendPasswordResetLinkIfAccountExists(String email) {
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

    /** Rzuca wyjatek, jesli token nie istnieje albo wygasl - frontend sprawdza link przed pokazaniem formularza. */
    public void ensurePasswordResetTokenIsValid(String resetToken) {
        findValidPasswordResetToken(resetToken);
    }

    /** Ustawia nowe haslo i kasuje token - link dziala tylko raz. */
    @Transactional
    public void resetPasswordUsingToken(String resetToken, String newPlainTextPassword) {
        PasswordResetToken passwordResetToken = findValidPasswordResetToken(resetToken);

        UserAccount userAccount = passwordResetToken.getUserAccount();
        userAccount.setPasswordHash(passwordEncoder.encode(newPlainTextPassword));
        userAccountRepository.save(userAccount);

        passwordResetTokenRepository.delete(passwordResetToken);
    }

    private PasswordResetToken findValidPasswordResetToken(String resetToken) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(resetToken)
                .orElseThrow(() -> new TokenNotFoundException("Token not found"));

        if (passwordResetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token has expired");
        }
        return passwordResetToken;
    }
}

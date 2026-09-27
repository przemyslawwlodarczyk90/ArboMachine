package com.example.arbomachine.domain.user.service.userauthenticationfacade;

import com.example.arbomachine.domain.user.entity.EmailConfirmationToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.TokenExpiredException;
import com.example.arbomachine.domain.user.exception.TokenNotFoundException;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Aktywacja konta po kliknieciu linku z maila rejestracyjnego. */
@Component
public class ConfirmEmailUsingTokenHandler {
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private final UserAccountRepository userAccountRepository;

    public ConfirmEmailUsingTokenHandler(EmailConfirmationTokenRepository emailConfirmationTokenRepository,
                                         UserAccountRepository userAccountRepository) {
        this.emailConfirmationTokenRepository = emailConfirmationTokenRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /** Oznacza e-mail jako potwierdzony i kasuje token - link dziala tylko raz. */
    public void handle(String token) {
        EmailConfirmationToken emailConfirmationToken = emailConfirmationTokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Token not found"));

        if (emailConfirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token has expired");
        }

        UserAccount userAccount = emailConfirmationToken.getUserAccount();
        userAccount.setEmailConfirmed(true);
        userAccountRepository.save(userAccount);

        emailConfirmationTokenRepository.delete(emailConfirmationToken);
    }
}

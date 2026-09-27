package com.example.arbomachine.domain.user.service.passwordresetfacade;

import com.example.arbomachine.domain.user.entity.PasswordResetToken;
import com.example.arbomachine.domain.user.exception.TokenExpiredException;
import com.example.arbomachine.domain.user.exception.TokenNotFoundException;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Wspolny handler: token resetu, ktory istnieje i nie wygasl - w przeciwnym razie wyjatek. */
@Component
public class FindValidPasswordResetTokenHandler {
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public FindValidPasswordResetTokenHandler(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public PasswordResetToken handle(String resetToken) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(resetToken)
                .orElseThrow(() -> new TokenNotFoundException("Token not found"));

        if (passwordResetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token has expired");
        }
        return passwordResetToken;
    }
}

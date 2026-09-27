package com.example.arbomachine.domain.user.service.userauthenticationfacade;

import com.example.arbomachine.domain.user.entity.EmailConfirmationToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.TokenExpiredException;
import com.example.arbomachine.domain.user.exception.TokenNotFoundException;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfirmEmailUsingTokenHandlerTest {

    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository = mock(EmailConfirmationTokenRepository.class);
    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final ConfirmEmailUsingTokenHandler confirmEmailUsingTokenHandler =
            new ConfirmEmailUsingTokenHandler(emailConfirmationTokenRepository, userAccountRepository);

    private EmailConfirmationToken tokenExpiringAt(LocalDateTime expiresAt, UserAccount tokenOwnerAccount) {
        return EmailConfirmationToken.builder()
                .token("confirmation-token")
                .createdAt(expiresAt.minusDays(1))
                .expiresAt(expiresAt)
                .userAccount(tokenOwnerAccount)
                .build();
    }

    @Test
    void validTokenConfirmsEmailAndIsDeleted() {
        UserAccount unconfirmedAccount = new UserAccount();
        EmailConfirmationToken validToken = tokenExpiringAt(LocalDateTime.now().plusDays(1), unconfirmedAccount);
        when(emailConfirmationTokenRepository.findByToken("confirmation-token")).thenReturn(Optional.of(validToken));

        confirmEmailUsingTokenHandler.handle("confirmation-token");

        assertTrue(unconfirmedAccount.isEmailConfirmed());
        verify(userAccountRepository).save(unconfirmedAccount);
        verify(emailConfirmationTokenRepository).delete(validToken);
    }

    @Test
    void unknownTokenIsRejected() {
        when(emailConfirmationTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundException.class, () -> confirmEmailUsingTokenHandler.handle("unknown-token"));
    }

    @Test
    void expiredTokenIsRejectedAndAccountStaysUnconfirmed() {
        EmailConfirmationToken expiredToken = tokenExpiringAt(LocalDateTime.now().minusDays(1), new UserAccount());
        when(emailConfirmationTokenRepository.findByToken("confirmation-token")).thenReturn(Optional.of(expiredToken));

        assertThrows(TokenExpiredException.class, () -> confirmEmailUsingTokenHandler.handle("confirmation-token"));
        verify(userAccountRepository, never()).save(any());
    }
}

package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.entity.EmailConfirmationToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.TokenExpiredException;
import com.example.arbomachine.domain.user.exception.TokenNotFoundException;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailConfirmationServiceTest {

    @Mock
    private EmailConfirmationTokenRepository emailConfirmationTokenRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private EmailConfirmationService emailConfirmationService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

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

        emailConfirmationService.confirmEmailUsingToken("confirmation-token");

        assertTrue(unconfirmedAccount.isEmailConfirmed());
        verify(userAccountRepository).save(unconfirmedAccount);
        verify(emailConfirmationTokenRepository).delete(validToken);
    }

    @Test
    void unknownTokenIsRejected() {
        when(emailConfirmationTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundException.class, () -> emailConfirmationService.confirmEmailUsingToken("unknown-token"));
    }

    @Test
    void expiredTokenIsRejectedAndAccountStaysUnconfirmed() {
        UserAccount unconfirmedAccount = new UserAccount();
        EmailConfirmationToken expiredToken = tokenExpiringAt(LocalDateTime.now().minusDays(1), unconfirmedAccount);
        when(emailConfirmationTokenRepository.findByToken("confirmation-token")).thenReturn(Optional.of(expiredToken));

        assertThrows(TokenExpiredException.class, () -> emailConfirmationService.confirmEmailUsingToken("confirmation-token"));
        verify(userAccountRepository, never()).save(any());
    }
}

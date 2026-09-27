package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserAccountDeletionServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private EmailConfirmationTokenRepository emailConfirmationTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private UserAccountDeletionService userAccountDeletionService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deletesTokensBeforeTheAccount() {
        UserAccount accountToDelete = UserAccount.builder().id(1L).build();
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(accountToDelete));

        userAccountDeletionService.deleteUserAccountById(1L);

        InOrder deletionOrder = inOrder(emailConfirmationTokenRepository, passwordResetTokenRepository, userAccountRepository);
        deletionOrder.verify(emailConfirmationTokenRepository).deleteByUserAccount(accountToDelete);
        deletionOrder.verify(passwordResetTokenRepository).deleteByUserAccount(accountToDelete);
        deletionOrder.verify(userAccountRepository).delete(accountToDelete);
    }

    @Test
    void missingAccountIsReportedAndNothingIsDeleted() {
        when(userAccountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserAccountNotFoundException.class, () -> userAccountDeletionService.deleteUserAccountById(1L));
        verify(emailConfirmationTokenRepository, never()).deleteByUserAccount(any());
        verify(userAccountRepository, never()).delete(any());
    }
}

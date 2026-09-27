package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountDeletionBlockedException;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserAccountDeletionServiceTest {

    private UserAccountRepository userAccountRepository;
    private EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private UserAccountDeletionGuard userAccountDeletionGuard;
    private UserAccountDeletionService userAccountDeletionService;

    @BeforeEach
    void createServiceWithOneDeletionGuard() {
        userAccountRepository = mock(UserAccountRepository.class);
        emailConfirmationTokenRepository = mock(EmailConfirmationTokenRepository.class);
        passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
        userAccountDeletionGuard = mock(UserAccountDeletionGuard.class);
        userAccountDeletionService = new UserAccountDeletionService(userAccountRepository,
                emailConfirmationTokenRepository, passwordResetTokenRepository, List.of(userAccountDeletionGuard));
    }

    @Test
    void asksGuardsAndDeletesTokensBeforeTheAccount() {
        UserAccount accountToDelete = UserAccount.builder().id(1L).build();
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(accountToDelete));

        userAccountDeletionService.deleteUserAccountById(1L);

        InOrder deletionOrder = inOrder(userAccountDeletionGuard, emailConfirmationTokenRepository,
                passwordResetTokenRepository, userAccountRepository);
        deletionOrder.verify(userAccountDeletionGuard).ensureUserAccountCanBeDeleted(accountToDelete);
        deletionOrder.verify(emailConfirmationTokenRepository).deleteByUserAccount(accountToDelete);
        deletionOrder.verify(passwordResetTokenRepository).deleteByUserAccount(accountToDelete);
        deletionOrder.verify(userAccountRepository).delete(accountToDelete);
    }

    @Test
    void guardCanBlockDeletionAndNothingIsDeleted() {
        UserAccount projectOwnerAccount = UserAccount.builder().id(2L).build();
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(projectOwnerAccount));
        doThrow(new UserAccountDeletionBlockedException("blocked"))
                .when(userAccountDeletionGuard).ensureUserAccountCanBeDeleted(projectOwnerAccount);

        assertThrows(UserAccountDeletionBlockedException.class, () -> userAccountDeletionService.deleteUserAccountById(2L));
        verify(emailConfirmationTokenRepository, never()).deleteByUserAccount(any());
        verify(userAccountRepository, never()).delete(any());
    }

    @Test
    void missingAccountIsReportedAndNothingIsDeleted() {
        when(userAccountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserAccountNotFoundException.class, () -> userAccountDeletionService.deleteUserAccountById(1L));
        verify(emailConfirmationTokenRepository, never()).deleteByUserAccount(any());
        verify(userAccountRepository, never()).delete(any());
    }
}

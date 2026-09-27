package com.example.arbomachine.domain.user.service.useraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.exception.UserAccountDeletionBlockedException;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Handlery fasady UserAccountFacade (wlasne konto) i ich wspolne handlery pomocnicze. */
class UserAccountHandlersTest {

    private static final String CURRENT_USER_EMAIL = "john.doe@example.com";

    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository = mock(EmailConfirmationTokenRepository.class);
    private final PasswordResetTokenRepository passwordResetTokenRepository = mock(PasswordResetTokenRepository.class);
    private final UserAccountDeletionGuard userAccountDeletionGuard = mock(UserAccountDeletionGuard.class);

    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler = new FindUserAccountByEmailHandler(userAccountRepository);
    private final GetCurrentUserAccountHandler getCurrentUserAccountHandler =
            new GetCurrentUserAccountHandler(findUserAccountByEmailHandler, new UserAccountMapper());
    private final UpdateUserProfileHandler updateUserProfileHandler = new UpdateUserProfileHandler(userAccountRepository);
    private final UpdateCurrentUserProfileHandler updateCurrentUserProfileHandler = new UpdateCurrentUserProfileHandler(
            findUserAccountByEmailHandler, updateUserProfileHandler, getCurrentUserAccountHandler);
    private final DeleteUserAccountByIdHandler deleteUserAccountByIdHandler = new DeleteUserAccountByIdHandler(
            userAccountRepository, emailConfirmationTokenRepository, passwordResetTokenRepository, List.of(userAccountDeletionGuard));
    private final DeleteCurrentUserAccountHandler deleteCurrentUserAccountHandler =
            new DeleteCurrentUserAccountHandler(findUserAccountByEmailHandler, deleteUserAccountByIdHandler);

    private UserAccount storedAccount;

    @BeforeEach
    void givenStoredOfficialAccount() {
        storedAccount = UserAccount.builder()
                .id(1L).firstName("John").lastName("Doe").email(CURRENT_USER_EMAIL)
                .phoneNumber("123456789").street("Street 1").postalCode("00-001").city("City")
                .role(UserRole.OFFICIAL).emailConfirmed(true)
                .build();
        when(userAccountRepository.findByEmail(CURRENT_USER_EMAIL)).thenReturn(Optional.of(storedAccount));
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(storedAccount));
    }

    @Test
    void currentUserAccountIsResolvedFromEmail() {
        UserAccountDto currentAccount = getCurrentUserAccountHandler.handle(CURRENT_USER_EMAIL);

        assertEquals(1L, currentAccount.id());
        assertEquals(UserRole.OFFICIAL, currentAccount.role());
    }

    @Test
    void unknownEmailIsReported() {
        when(userAccountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UserAccountNotFoundException.class, () -> getCurrentUserAccountHandler.handle("missing@example.com"));
    }

    @Test
    void ownProfileUpdateOverwritesFieldsButKeepsEmail() {
        UserProfileDto updatedProfile = new UserProfileDto(null, "John", "Doe", CURRENT_USER_EMAIL,
                "987654321", "New Street", "00-002", "New City");

        UserAccountDto updatedAccount = updateCurrentUserProfileHandler.handle(CURRENT_USER_EMAIL, updatedProfile);

        verify(userAccountRepository).save(storedAccount);
        assertEquals("New Street", updatedAccount.street());
        assertEquals("00-002", storedAccount.getPostalCode());
        assertEquals(CURRENT_USER_EMAIL, storedAccount.getEmail());
    }

    @Test
    void changingEmailIsRejected() {
        UserProfileDto profileWithDifferentEmail = new UserProfileDto(null, "John", "Doe", "new.email@example.com",
                null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> updateUserProfileHandler.handle(1L, profileWithDifferentEmail));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void ownAccountDeletionAsksGuardsAndDeletesTokensBeforeTheAccount() {
        deleteCurrentUserAccountHandler.handle(CURRENT_USER_EMAIL);

        InOrder deletionOrder = inOrder(userAccountDeletionGuard, emailConfirmationTokenRepository,
                passwordResetTokenRepository, userAccountRepository);
        deletionOrder.verify(userAccountDeletionGuard).ensureUserAccountCanBeDeleted(storedAccount);
        deletionOrder.verify(emailConfirmationTokenRepository).deleteByUserAccount(storedAccount);
        deletionOrder.verify(passwordResetTokenRepository).deleteByUserAccount(storedAccount);
        deletionOrder.verify(userAccountRepository).delete(storedAccount);
    }

    @Test
    void guardCanBlockDeletionAndNothingIsDeleted() {
        doThrow(new UserAccountDeletionBlockedException("blocked"))
                .when(userAccountDeletionGuard).ensureUserAccountCanBeDeleted(storedAccount);

        assertThrows(UserAccountDeletionBlockedException.class, () -> deleteUserAccountByIdHandler.handle(1L));
        verify(emailConfirmationTokenRepository, never()).deleteByUserAccount(any());
        verify(userAccountRepository, never()).delete(any());
    }

    @Test
    void deletingMissingAccountIsReported() {
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserAccountNotFoundException.class, () -> deleteUserAccountByIdHandler.handle(99L));
        verify(userAccountRepository, never()).delete(any());
    }
}

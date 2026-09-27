package com.example.arbomachine.domain.user.service.adminuseraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.exception.AdminAccountModificationForbiddenException;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.domain.user.service.useraccountfacade.DeleteUserAccountByIdHandler;
import com.example.arbomachine.domain.user.service.useraccountfacade.UpdateUserProfileHandler;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Handlery fasady AdminUserAccountFacade. */
class AdminUserAccountHandlersTest {

    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final UserAccountMapper userAccountMapper = new UserAccountMapper();
    private final UpdateUserProfileHandler updateUserProfileHandler = mock(UpdateUserProfileHandler.class);
    private final DeleteUserAccountByIdHandler deleteUserAccountByIdHandler = mock(DeleteUserAccountByIdHandler.class);

    private final FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler =
            new FindUserAccountManageableByAdminHandler(userAccountRepository);
    private final ChangeUserRoleHandler changeUserRoleHandler =
            new ChangeUserRoleHandler(findUserAccountManageableByAdminHandler, userAccountRepository, userAccountMapper);
    private final SetAccountLockedByAdminHandler setAccountLockedByAdminHandler =
            new SetAccountLockedByAdminHandler(findUserAccountManageableByAdminHandler, userAccountRepository, userAccountMapper);
    private final ConfirmEmailOnUsersBehalfHandler confirmEmailOnUsersBehalfHandler =
            new ConfirmEmailOnUsersBehalfHandler(findUserAccountManageableByAdminHandler, userAccountRepository, userAccountMapper);
    private final DeleteUserAccountAsAdminHandler deleteUserAccountAsAdminHandler =
            new DeleteUserAccountAsAdminHandler(findUserAccountManageableByAdminHandler, deleteUserAccountByIdHandler);
    private final UpdateUserProfileAsAdminHandler updateUserProfileAsAdminHandler = new UpdateUserProfileAsAdminHandler(
            findUserAccountManageableByAdminHandler, updateUserProfileHandler,
            new GetUserAccountByIdHandler(userAccountRepository, userAccountMapper));

    @BeforeEach
    void saveReturnsTheSameAccount() {
        // save() zwraca to, co dostal - jak prawdziwe JPA dla juz istniejacej encji
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private UserAccount givenStoredAccount(long userAccountId, UserRole role) {
        UserAccount storedAccount = UserAccount.builder()
                .id(userAccountId).email("user" + userAccountId + "@example.com").role(role).emailConfirmed(true)
                .build();
        when(userAccountRepository.findById(userAccountId)).thenReturn(Optional.of(storedAccount));
        return storedAccount;
    }

    @Test
    void changesArboristIntoOfficial() {
        UserAccount arboristAccount = givenStoredAccount(1L, UserRole.ARBORIST);

        UserAccountDto updatedAccount = changeUserRoleHandler.handle(1L, AssignableUserRole.OFFICIAL);

        assertEquals(UserRole.OFFICIAL, updatedAccount.role());
        verify(userAccountRepository).save(arboristAccount);
    }

    @Test
    void locksAccountAndConfirmsEmailOnUsersBehalf() {
        UserAccount unconfirmedOfficialAccount = givenStoredAccount(2L, UserRole.OFFICIAL);
        unconfirmedOfficialAccount.setEmailConfirmed(false);

        assertTrue(setAccountLockedByAdminHandler.handle(2L, true).lockedByAdmin());
        assertTrue(confirmEmailOnUsersBehalfHandler.handle(2L).emailConfirmed());
    }

    @Test
    void deletionIsDelegatedToSharedDeletionHandler() {
        givenStoredAccount(3L, UserRole.ARBORIST);

        deleteUserAccountAsAdminHandler.handle(3L);

        verify(deleteUserAccountByIdHandler).handle(3L);
    }

    @Test
    void adminAccountsCannotBeModifiedFromTheApplication() {
        givenStoredAccount(9L, UserRole.ADMIN);

        assertThrows(AdminAccountModificationForbiddenException.class, () -> changeUserRoleHandler.handle(9L, AssignableUserRole.ARBORIST));
        assertThrows(AdminAccountModificationForbiddenException.class, () -> setAccountLockedByAdminHandler.handle(9L, true));
        assertThrows(AdminAccountModificationForbiddenException.class, () -> confirmEmailOnUsersBehalfHandler.handle(9L));
        assertThrows(AdminAccountModificationForbiddenException.class, () -> updateUserProfileAsAdminHandler.handle(9L, null));
        assertThrows(AdminAccountModificationForbiddenException.class, () -> deleteUserAccountAsAdminHandler.handle(9L));
        verifyNoInteractions(deleteUserAccountByIdHandler, updateUserProfileHandler);
        verify(userAccountRepository, never()).save(any());
    }
}

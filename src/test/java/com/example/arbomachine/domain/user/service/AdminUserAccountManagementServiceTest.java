package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.exception.AdminAccountModificationForbiddenException;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminUserAccountManagementServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Spy
    private UserAccountMapper userAccountMapper = new UserAccountMapper();

    @Mock
    private UserProfileUpdateService userProfileUpdateService;

    @Mock
    private UserAccountDeletionService userAccountDeletionService;

    @InjectMocks
    private AdminUserAccountManagementService adminUserAccountManagementService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
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

        UserAccountDto updatedAccount = adminUserAccountManagementService.changeUserRole(1L, AssignableUserRole.OFFICIAL);

        assertEquals(UserRole.OFFICIAL, updatedAccount.role());
        verify(userAccountRepository).save(arboristAccount);
    }

    @Test
    void locksAccountAndConfirmsEmailOnUsersBehalf() {
        UserAccount unconfirmedOfficialAccount = givenStoredAccount(2L, UserRole.OFFICIAL);
        unconfirmedOfficialAccount.setEmailConfirmed(false);

        assertTrue(adminUserAccountManagementService.setAccountLockedByAdmin(2L, true).lockedByAdmin());
        assertTrue(adminUserAccountManagementService.confirmEmailOnUsersBehalf(2L).emailConfirmed());
    }

    @Test
    void deletionIsDelegatedToDeletionService() {
        givenStoredAccount(3L, UserRole.ARBORIST);

        adminUserAccountManagementService.deleteUserAccountAsAdmin(3L);

        verify(userAccountDeletionService).deleteUserAccountById(3L);
    }

    @Test
    void adminAccountsCannotBeModifiedFromTheApplication() {
        givenStoredAccount(9L, UserRole.ADMIN);

        assertThrows(AdminAccountModificationForbiddenException.class,
                () -> adminUserAccountManagementService.changeUserRole(9L, AssignableUserRole.ARBORIST));
        assertThrows(AdminAccountModificationForbiddenException.class,
                () -> adminUserAccountManagementService.setAccountLockedByAdmin(9L, true));
        assertThrows(AdminAccountModificationForbiddenException.class,
                () -> adminUserAccountManagementService.confirmEmailOnUsersBehalf(9L));
        assertThrows(AdminAccountModificationForbiddenException.class,
                () -> adminUserAccountManagementService.updateUserProfileAsAdmin(9L, null));
        assertThrows(AdminAccountModificationForbiddenException.class,
                () -> adminUserAccountManagementService.deleteUserAccountAsAdmin(9L));
        verifyNoInteractions(userAccountDeletionService, userProfileUpdateService);
        verify(userAccountRepository, never()).save(any());
    }
}

package com.example.arbomachine.domain.user.facade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserAccountStatisticsDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.service.adminuseraccountfacade.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Operacje admina na cudzych kontach (dostep tylko dla roli ADMIN - pilnuje SecurityConfig dla /api/admin/**).
 * Deleguje do handlerow z service/adminuseraccountfacade.
 */
@Component
public class AdminUserAccountFacade {
    private final ListUserAccountsHandler listUserAccountsHandler;
    private final GetUserAccountByIdHandler getUserAccountByIdHandler;
    private final GetUserAccountStatisticsHandler getUserAccountStatisticsHandler;
    private final UpdateUserProfileAsAdminHandler updateUserProfileAsAdminHandler;
    private final ChangeUserRoleHandler changeUserRoleHandler;
    private final SetAccountLockedByAdminHandler setAccountLockedByAdminHandler;
    private final ConfirmEmailOnUsersBehalfHandler confirmEmailOnUsersBehalfHandler;
    private final DeleteUserAccountAsAdminHandler deleteUserAccountAsAdminHandler;

    public AdminUserAccountFacade(ListUserAccountsHandler listUserAccountsHandler,
                                  GetUserAccountByIdHandler getUserAccountByIdHandler,
                                  GetUserAccountStatisticsHandler getUserAccountStatisticsHandler,
                                  UpdateUserProfileAsAdminHandler updateUserProfileAsAdminHandler,
                                  ChangeUserRoleHandler changeUserRoleHandler,
                                  SetAccountLockedByAdminHandler setAccountLockedByAdminHandler,
                                  ConfirmEmailOnUsersBehalfHandler confirmEmailOnUsersBehalfHandler,
                                  DeleteUserAccountAsAdminHandler deleteUserAccountAsAdminHandler) {
        this.listUserAccountsHandler = listUserAccountsHandler;
        this.getUserAccountByIdHandler = getUserAccountByIdHandler;
        this.getUserAccountStatisticsHandler = getUserAccountStatisticsHandler;
        this.updateUserProfileAsAdminHandler = updateUserProfileAsAdminHandler;
        this.changeUserRoleHandler = changeUserRoleHandler;
        this.setAccountLockedByAdminHandler = setAccountLockedByAdminHandler;
        this.confirmEmailOnUsersBehalfHandler = confirmEmailOnUsersBehalfHandler;
        this.deleteUserAccountAsAdminHandler = deleteUserAccountAsAdminHandler;
    }

    public List<UserAccountDto> listUserAccounts(UserRole roleFilter) {
        return listUserAccountsHandler.handle(roleFilter);
    }

    public UserAccountDto getUserAccountById(Long userAccountId) {
        return getUserAccountByIdHandler.handle(userAccountId);
    }

    public UserAccountStatisticsDto getUserAccountStatistics() {
        return getUserAccountStatisticsHandler.handle();
    }

    public UserAccountDto updateUserProfileAsAdmin(Long userAccountId, UserProfileDto updatedProfile) {
        return updateUserProfileAsAdminHandler.handle(userAccountId, updatedProfile);
    }

    public UserAccountDto changeUserRole(Long userAccountId, AssignableUserRole newRole) {
        return changeUserRoleHandler.handle(userAccountId, newRole);
    }

    public UserAccountDto setAccountLockedByAdmin(Long userAccountId, boolean lockedByAdmin) {
        return setAccountLockedByAdminHandler.handle(userAccountId, lockedByAdmin);
    }

    public UserAccountDto confirmEmailOnUsersBehalf(Long userAccountId) {
        return confirmEmailOnUsersBehalfHandler.handle(userAccountId);
    }

    public void deleteUserAccountAsAdmin(Long userAccountId) {
        deleteUserAccountAsAdminHandler.handle(userAccountId);
    }
}

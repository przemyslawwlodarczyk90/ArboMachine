package com.example.arbomachine.domain.user.service.adminuseraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.service.useraccountfacade.UpdateUserProfileHandler;
import org.springframework.stereotype.Component;

/** Edycja danych profilu cudzego konta przez admina (nie dotyczy kont administratorow). */
@Component
public class UpdateUserProfileAsAdminHandler {
    private final FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler;
    private final UpdateUserProfileHandler updateUserProfileHandler;
    private final GetUserAccountByIdHandler getUserAccountByIdHandler;

    public UpdateUserProfileAsAdminHandler(FindUserAccountManageableByAdminHandler findUserAccountManageableByAdminHandler,
                                           UpdateUserProfileHandler updateUserProfileHandler,
                                           GetUserAccountByIdHandler getUserAccountByIdHandler) {
        this.findUserAccountManageableByAdminHandler = findUserAccountManageableByAdminHandler;
        this.updateUserProfileHandler = updateUserProfileHandler;
        this.getUserAccountByIdHandler = getUserAccountByIdHandler;
    }

    public UserAccountDto handle(Long userAccountId, UserProfileDto updatedProfile) {
        findUserAccountManageableByAdminHandler.handle(userAccountId);
        updateUserProfileHandler.handle(userAccountId, updatedProfile);
        return getUserAccountByIdHandler.handle(userAccountId);
    }
}

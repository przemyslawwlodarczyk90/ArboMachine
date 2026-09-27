package com.example.arbomachine.domain.user.facade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.service.useraccountfacade.DeleteCurrentUserAccountHandler;
import com.example.arbomachine.domain.user.service.useraccountfacade.GetCurrentUserAccountHandler;
import com.example.arbomachine.domain.user.service.useraccountfacade.UpdateCurrentUserProfileHandler;
import org.springframework.stereotype.Component;

/**
 * WLASNE konto zalogowanego uzytkownika (/api/users/me). Konto wskazuje e-mail z tokenu JWT,
 * wiec nie da sie tu podac cudzego id. Tylko deleguje do handlerow z service/useraccountfacade.
 */
@Component
public class UserAccountFacade {
    private final GetCurrentUserAccountHandler getCurrentUserAccountHandler;
    private final UpdateCurrentUserProfileHandler updateCurrentUserProfileHandler;
    private final DeleteCurrentUserAccountHandler deleteCurrentUserAccountHandler;

    public UserAccountFacade(GetCurrentUserAccountHandler getCurrentUserAccountHandler,
                             UpdateCurrentUserProfileHandler updateCurrentUserProfileHandler,
                             DeleteCurrentUserAccountHandler deleteCurrentUserAccountHandler) {
        this.getCurrentUserAccountHandler = getCurrentUserAccountHandler;
        this.updateCurrentUserProfileHandler = updateCurrentUserProfileHandler;
        this.deleteCurrentUserAccountHandler = deleteCurrentUserAccountHandler;
    }

    public UserAccountDto getCurrentUserAccount(String currentUserEmail) {
        return getCurrentUserAccountHandler.handle(currentUserEmail);
    }

    public UserAccountDto updateCurrentUserProfile(String currentUserEmail, UserProfileDto updatedProfile) {
        return updateCurrentUserProfileHandler.handle(currentUserEmail, updatedProfile);
    }

    public void deleteCurrentUserAccount(String currentUserEmail) {
        deleteCurrentUserAccountHandler.handle(currentUserEmail);
    }
}

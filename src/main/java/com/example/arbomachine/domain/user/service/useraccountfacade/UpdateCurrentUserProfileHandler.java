package com.example.arbomachine.domain.user.service.useraccountfacade;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import org.springframework.stereotype.Component;

/** Edycja WLASNEGO profilu - konto wskazuje e-mail z tokenu JWT, wiec nie da sie podac cudzego id. */
@Component
public class UpdateCurrentUserProfileHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final UpdateUserProfileHandler updateUserProfileHandler;
    private final GetCurrentUserAccountHandler getCurrentUserAccountHandler;

    public UpdateCurrentUserProfileHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                           UpdateUserProfileHandler updateUserProfileHandler,
                                           GetCurrentUserAccountHandler getCurrentUserAccountHandler) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.updateUserProfileHandler = updateUserProfileHandler;
        this.getCurrentUserAccountHandler = getCurrentUserAccountHandler;
    }

    public UserAccountDto handle(String currentUserEmail, UserProfileDto updatedProfile) {
        Long currentUserAccountId = findUserAccountByEmailHandler.handle(currentUserEmail).getId();
        updateUserProfileHandler.handle(currentUserAccountId, updatedProfile);
        return getCurrentUserAccountHandler.handle(currentUserEmail);
    }
}

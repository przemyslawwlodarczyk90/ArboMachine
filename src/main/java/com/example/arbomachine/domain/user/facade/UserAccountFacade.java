package com.example.arbomachine.domain.user.facade;

import com.example.arbomachine.domain.user.dto.LoginRequestDto;
import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.user.service.*;
import org.springframework.stereotype.Service;

/**
 * Operacje, ktore uzytkownik wykonuje sam na sobie: rejestracja, logowanie i obsluga WLASNEGO konta.
 * Konto "biezacego uzytkownika" wskazuje e-mail z tokenu JWT, wiec nie da sie tu podac cudzego id.
 */
@Service
public class UserAccountFacade {
    private final UserRegistrationService userRegistrationService;
    private final UserLoginService userLoginService;
    private final UserProfileUpdateService userProfileUpdateService;
    private final UserAccountDeletionService userAccountDeletionService;
    private final UserAccountQueryService userAccountQueryService;

    public UserAccountFacade(UserRegistrationService userRegistrationService,
                             UserLoginService userLoginService,
                             UserProfileUpdateService userProfileUpdateService,
                             UserAccountDeletionService userAccountDeletionService,
                             UserAccountQueryService userAccountQueryService) {
        this.userRegistrationService = userRegistrationService;
        this.userLoginService = userLoginService;
        this.userProfileUpdateService = userProfileUpdateService;
        this.userAccountDeletionService = userAccountDeletionService;
        this.userAccountQueryService = userAccountQueryService;
    }

    public void registerNewUserAccount(UserProfileDto userProfile, String plainTextPassword,
                                       AssignableUserRole requestedRole) {
        userRegistrationService.registerNewUserAccount(userProfile, plainTextPassword, requestedRole);
    }

    public String loginAndIssueAccessToken(LoginRequestDto loginRequest) {
        return userLoginService.loginAndIssueAccessToken(loginRequest);
    }

    public UserAccountDto getCurrentUserAccount(String currentUserEmail) {
        return userAccountQueryService.getUserAccountByEmail(currentUserEmail);
    }

    public UserAccountDto updateCurrentUserProfile(String currentUserEmail, UserProfileDto updatedProfile) {
        Long currentUserAccountId = userAccountQueryService.getUserAccountEntityByEmail(currentUserEmail).getId();
        userProfileUpdateService.updateUserProfile(currentUserAccountId, updatedProfile);
        return userAccountQueryService.getUserAccountByEmail(currentUserEmail);
    }

    public void deleteCurrentUserAccount(String currentUserEmail) {
        Long currentUserAccountId = userAccountQueryService.getUserAccountEntityByEmail(currentUserEmail).getId();
        userAccountDeletionService.deleteUserAccountById(currentUserAccountId);
    }
}

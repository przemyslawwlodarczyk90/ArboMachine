package com.example.arbomachine.domain.user.facade;

import com.example.arbomachine.domain.user.dto.LoginRequestDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.user.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UserAccountFacadeTest {

    private static final String CURRENT_USER_EMAIL = "jan.kowalski@example.com";

    @Mock
    private UserRegistrationService userRegistrationService;

    @Mock
    private UserLoginService userLoginService;

    @Mock
    private UserProfileUpdateService userProfileUpdateService;

    @Mock
    private UserAccountDeletionService userAccountDeletionService;

    @Mock
    private UserAccountQueryService userAccountQueryService;

    @InjectMocks
    private UserAccountFacade userAccountFacade;

    private final UserProfileDto userProfile = UserProfileDto.builder()
            .firstName("Jan")
            .lastName("Kowalski")
            .email(CURRENT_USER_EMAIL)
            .build();

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
        when(userAccountQueryService.getUserAccountEntityByEmail(CURRENT_USER_EMAIL))
                .thenReturn(UserAccount.builder().id(42L).email(CURRENT_USER_EMAIL).build());
    }

    @Test
    void registrationIsDelegatedWithChosenRole() {
        userAccountFacade.registerNewUserAccount(userProfile, "password", AssignableUserRole.OFFICIAL);

        verify(userRegistrationService).registerNewUserAccount(userProfile, "password", AssignableUserRole.OFFICIAL);
    }

    @Test
    void loginReturnsTokenFromLoginService() {
        LoginRequestDto loginRequest = new LoginRequestDto(CURRENT_USER_EMAIL, "password");
        when(userLoginService.loginAndIssueAccessToken(loginRequest)).thenReturn("token");

        assertEquals("token", userAccountFacade.loginAndIssueAccessToken(loginRequest));
    }

    @Test
    void profileUpdateTargetsTheAccountResolvedFromCurrentUserEmail() {
        userAccountFacade.updateCurrentUserProfile(CURRENT_USER_EMAIL, userProfile);

        verify(userProfileUpdateService).updateUserProfile(42L, userProfile);
    }

    @Test
    void deletionTargetsTheAccountResolvedFromCurrentUserEmail() {
        userAccountFacade.deleteCurrentUserAccount(CURRENT_USER_EMAIL);

        verify(userAccountDeletionService).deleteUserAccountById(42L);
    }
}

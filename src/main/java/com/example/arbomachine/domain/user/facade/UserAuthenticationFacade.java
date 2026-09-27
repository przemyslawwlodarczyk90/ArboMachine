package com.example.arbomachine.domain.user.facade;

import com.example.arbomachine.domain.user.dto.LoginRequestDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.user.service.userauthenticationfacade.ConfirmEmailUsingTokenHandler;
import com.example.arbomachine.domain.user.service.userauthenticationfacade.LoginAndIssueAccessTokenHandler;
import com.example.arbomachine.domain.user.service.userauthenticationfacade.RegisterNewUserAccountHandler;
import org.springframework.stereotype.Component;

/** Publiczne wejscie do systemu: rejestracja, potwierdzenie e-maila i logowanie. Deleguje do service/userauthenticationfacade. */
@Component
public class UserAuthenticationFacade {
    private final RegisterNewUserAccountHandler registerNewUserAccountHandler;
    private final ConfirmEmailUsingTokenHandler confirmEmailUsingTokenHandler;
    private final LoginAndIssueAccessTokenHandler loginAndIssueAccessTokenHandler;

    public UserAuthenticationFacade(RegisterNewUserAccountHandler registerNewUserAccountHandler,
                                    ConfirmEmailUsingTokenHandler confirmEmailUsingTokenHandler,
                                    LoginAndIssueAccessTokenHandler loginAndIssueAccessTokenHandler) {
        this.registerNewUserAccountHandler = registerNewUserAccountHandler;
        this.confirmEmailUsingTokenHandler = confirmEmailUsingTokenHandler;
        this.loginAndIssueAccessTokenHandler = loginAndIssueAccessTokenHandler;
    }

    public void registerNewUserAccount(UserProfileDto userProfile, String plainTextPassword, AssignableUserRole requestedRole) {
        registerNewUserAccountHandler.handle(userProfile, plainTextPassword, requestedRole);
    }

    public void confirmEmailUsingToken(String token) {
        confirmEmailUsingTokenHandler.handle(token);
    }

    public String loginAndIssueAccessToken(LoginRequestDto loginRequest) {
        return loginAndIssueAccessTokenHandler.handle(loginRequest);
    }
}

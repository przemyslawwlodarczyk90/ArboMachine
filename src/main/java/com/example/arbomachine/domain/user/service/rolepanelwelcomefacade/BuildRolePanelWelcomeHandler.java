package com.example.arbomachine.domain.user.service.rolepanelwelcomefacade;

import com.example.arbomachine.domain.user.dto.RolePanelWelcomeDto;
import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.service.useraccountfacade.GetCurrentUserAccountHandler;
import org.springframework.stereotype.Component;

/** Dane powitalne panelu dla zalogowanego uzytkownika; tekst powitania podaje kontroler danego panelu. */
@Component
public class BuildRolePanelWelcomeHandler {
    private final GetCurrentUserAccountHandler getCurrentUserAccountHandler;

    public BuildRolePanelWelcomeHandler(GetCurrentUserAccountHandler getCurrentUserAccountHandler) {
        this.getCurrentUserAccountHandler = getCurrentUserAccountHandler;
    }

    public RolePanelWelcomeDto handle(String currentUserEmail, String welcomeMessage) {
        UserAccountDto currentUserAccount = getCurrentUserAccountHandler.handle(currentUserEmail);
        return new RolePanelWelcomeDto(currentUserAccount.role(), currentUserAccount.firstName(),
                currentUserAccount.lastName(), welcomeMessage);
    }
}

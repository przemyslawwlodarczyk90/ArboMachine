package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.RolePanelWelcomeDto;
import com.example.arbomachine.domain.user.dto.UserAccountDto;
import org.springframework.stereotype.Service;

/**
 * Sklada dane powitalne panelu dla zalogowanego uzytkownika. Wspolne dla kontrolerow wszystkich paneli,
 * ale nie wie nic o konkretnym panelu - tekst powitania podaje wywolujacy kontroler.
 */
@Service
public class RolePanelWelcomeService {
    private final UserAccountQueryService userAccountQueryService;

    public RolePanelWelcomeService(UserAccountQueryService userAccountQueryService) {
        this.userAccountQueryService = userAccountQueryService;
    }

    public RolePanelWelcomeDto buildPanelWelcomeForUser(String currentUserEmail, String welcomeMessage) {
        UserAccountDto currentUserAccount = userAccountQueryService.getUserAccountByEmail(currentUserEmail);
        return new RolePanelWelcomeDto(currentUserAccount.role(), currentUserAccount.firstName(),
                currentUserAccount.lastName(), welcomeMessage);
    }
}

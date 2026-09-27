package com.example.arbomachine.domain.user.facade;

import com.example.arbomachine.domain.user.dto.RolePanelWelcomeDto;
import com.example.arbomachine.domain.user.service.rolepanelwelcomefacade.BuildRolePanelWelcomeHandler;
import org.springframework.stereotype.Component;

/** Dane powitalne paneli rol - uzywana przez kontrolery paneli arborysty, urzednika i admina. */
@Component
public class RolePanelWelcomeFacade {
    private final BuildRolePanelWelcomeHandler buildRolePanelWelcomeHandler;

    public RolePanelWelcomeFacade(BuildRolePanelWelcomeHandler buildRolePanelWelcomeHandler) {
        this.buildRolePanelWelcomeHandler = buildRolePanelWelcomeHandler;
    }

    public RolePanelWelcomeDto buildPanelWelcomeForUser(String currentUserEmail, String welcomeMessage) {
        return buildRolePanelWelcomeHandler.handle(currentUserEmail, welcomeMessage);
    }
}

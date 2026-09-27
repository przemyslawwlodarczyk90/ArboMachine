package com.example.arbomachine.domain.user.service.rolepanelwelcomefacade;

import com.example.arbomachine.domain.user.dto.RolePanelWelcomeDto;
import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.service.useraccountfacade.GetCurrentUserAccountHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildRolePanelWelcomeHandlerTest {

    @Test
    void welcomeContainsCurrentUserRoleNameAndGivenMessage() {
        GetCurrentUserAccountHandler getCurrentUserAccountHandler = mock(GetCurrentUserAccountHandler.class);
        when(getCurrentUserAccountHandler.handle("jan@example.com")).thenReturn(new UserAccountDto(
                1L, "Jan", "Kowalski", "jan@example.com", null, null, null, null, UserRole.ARBORIST, true, false));

        RolePanelWelcomeDto panelWelcome = new BuildRolePanelWelcomeHandler(getCurrentUserAccountHandler)
                .handle("jan@example.com", "Hello");

        assertEquals(UserRole.ARBORIST, panelWelcome.role());
        assertEquals("Jan", panelWelcome.firstName());
        assertEquals("Kowalski", panelWelcome.lastName());
        assertEquals("Hello", panelWelcome.welcomeMessage());
    }
}

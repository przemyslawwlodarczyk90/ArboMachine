package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.RolePanelWelcomeDto;
import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RolePanelWelcomeServiceTest {

    @Test
    void welcomeContainsCurrentUserRoleNameAndGivenMessage() {
        UserAccountQueryService userAccountQueryService = mock(UserAccountQueryService.class);
        when(userAccountQueryService.getUserAccountByEmail("jan@example.com")).thenReturn(new UserAccountDto(
                1L, "Jan", "Kowalski", "jan@example.com", null, null, null, null, UserRole.ARBORIST, true, false));
        RolePanelWelcomeService rolePanelWelcomeService = new RolePanelWelcomeService(userAccountQueryService);

        RolePanelWelcomeDto panelWelcome = rolePanelWelcomeService.buildPanelWelcomeForUser("jan@example.com", "Hello");

        assertEquals(UserRole.ARBORIST, panelWelcome.role());
        assertEquals("Jan", panelWelcome.firstName());
        assertEquals("Kowalski", panelWelcome.lastName());
        assertEquals("Hello", panelWelcome.welcomeMessage());
    }
}

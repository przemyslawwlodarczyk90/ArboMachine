package com.example.arbomachine.infrastructure.api;

import com.example.arbomachine.domain.user.dto.LoginRequestDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.user.facade.UserAccountFacade;
import com.example.arbomachine.domain.user.service.EmailConfirmationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAccountControllerTest {

    private static final String CURRENT_USER_EMAIL = "jan.kowalski@example.com";

    /** Body rejestracji; %s = accountType. */
    private static final String REGISTRATION_REQUEST_TEMPLATE = """
            {
                "profile": {
                    "firstName": "Jan",
                    "lastName": "Kowalski",
                    "email": "jan.kowalski@example.com",
                    "phoneNumber": "+48123456789",
                    "street": "Ulica Główna",
                    "postalCode": "00-123",
                    "city": "Warszawa"
                },
                "password": "haslo123",
                "accountType": "%s"
            }
            """;

    private static final String PROFILE_UPDATE_REQUEST = """
            {
                "firstName": "Jan",
                "lastName": "Kowalski",
                "email": "jan.kowalski@example.com",
                "city": "Warszawa"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAccountFacade userAccountFacade;

    @MockBean
    private EmailConfirmationService emailConfirmationService;

    @Test
    void registersAccountWithChosenRole() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRATION_REQUEST_TEMPLATE.formatted("OFFICIAL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value("User registered successfully"));

        Mockito.verify(userAccountFacade)
                .registerNewUserAccount(any(UserProfileDto.class), eq("haslo123"), eq(AssignableUserRole.OFFICIAL));
    }

    @Test
    void registeringAsAdminIsRejected() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRATION_REQUEST_TEMPLATE.formatted("ADMIN")))
                .andExpect(status().isBadRequest());

        Mockito.verify(userAccountFacade, Mockito.never()).registerNewUserAccount(any(), anyString(), any());
    }

    @Test
    void registeringWithoutAccountTypeIsRejected() throws Exception {
        String requestWithoutAccountType = REGISTRATION_REQUEST_TEMPLATE.replaceAll(",\\s*\"accountType\": \"%s\"", "");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestWithoutAccountType))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accountType").value("Account type is mandatory"));
    }

    @Test
    void loginReturnsAccessToken() throws Exception {
        Mockito.when(userAccountFacade.loginAndIssueAccessToken(any(LoginRequestDto.class))).thenReturn("fake-jwt-token");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"jan.kowalski@example.com\", \"password\": \"haslo123\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("fake-jwt-token"));
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL, roles = "ARBORIST")
    void updatesOwnProfileOfAuthenticatedUser() throws Exception {
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFILE_UPDATE_REQUEST))
                .andExpect(status().isOk());

        Mockito.verify(userAccountFacade).updateCurrentUserProfile(eq(CURRENT_USER_EMAIL), any(UserProfileDto.class));
    }

    @Test
    @WithMockUser(username = CURRENT_USER_EMAIL, roles = "OFFICIAL")
    void deletesOwnAccountOfAuthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNoContent());

        Mockito.verify(userAccountFacade).deleteCurrentUserAccount(CURRENT_USER_EMAIL);
    }

    @Test
    void confirmsEmailWithTokenFromLink() throws Exception {
        mockMvc.perform(get("/api/users/confirm").param("token", "fake-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("User confirmed successfully"));

        Mockito.verify(emailConfirmationService).confirmEmailUsingToken("fake-token");
    }
}

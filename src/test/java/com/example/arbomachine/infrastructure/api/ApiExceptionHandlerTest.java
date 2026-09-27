package com.example.arbomachine.infrastructure.api;

import com.example.arbomachine.domain.user.exception.InvalidCredentialsException;
import com.example.arbomachine.domain.user.exception.TokenExpiredException;
import com.example.arbomachine.domain.user.exception.TokenNotFoundException;
import com.example.arbomachine.domain.user.service.EmailConfirmationService;
import com.example.arbomachine.domain.user.service.UserLoginService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sprawdza, jaki status HTTP i jaka tresc daje kazdy rodzaj bledu. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserLoginService userLoginService;

    @MockBean
    private EmailConfirmationService emailConfirmationService;

    @Test
    void validationErrorsAreReturnedAsFieldToMessageMap() throws Exception {
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"invalid\", \"password\": \"\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email should be valid"))
                .andExpect(jsonPath("$.password").value("Password is mandatory"));
    }

    @Test
    void unknownTokenGivesNotFound() throws Exception {
        Mockito.doThrow(new TokenNotFoundException("Token not found"))
                .when(emailConfirmationService).confirmEmailUsingToken("invalid-token");

        mockMvc.perform(get("/api/users/confirm").param("token", "invalid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("Token not found"));
    }

    @Test
    void expiredTokenGivesBadRequest() throws Exception {
        Mockito.doThrow(new TokenExpiredException("Token has expired"))
                .when(emailConfirmationService).confirmEmailUsingToken("expired-token");

        mockMvc.perform(get("/api/users/confirm").param("token", "expired-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Token has expired"));
    }

    @Test
    void missingAccountGivesNotFound() throws Exception {
        // pusta baza testowa - serwis admina nie znajdzie konta
        mockMvc.perform(delete("/api/admin/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("User not found"));
    }

    @Test
    void invalidCredentialsGiveUnauthorized() throws Exception {
        Mockito.doThrow(new InvalidCredentialsException("Invalid email or password"))
                .when(userLoginService).loginAndIssueAccessToken(Mockito.any());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"invalid@example.com\", \"password\": \"wrongpassword\" }"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value("Invalid email or password"));
    }

    @Test
    void unknownEnumValueGivesMalformedRequest() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("role", "NOT_A_ROLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Malformed request"));
    }
}

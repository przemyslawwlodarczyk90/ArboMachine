package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.LoginRequestDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.AccountLockedByAdminException;
import com.example.arbomachine.domain.user.exception.EmailNotConfirmedException;
import com.example.arbomachine.domain.user.exception.InvalidCredentialsException;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserLoginServiceTest {

    private static final String EMAIL = "jan.kowalski@example.com";
    private static final String CORRECT_PASSWORD = "password123";

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserLoginService userLoginService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
        when(passwordEncoder.matches(CORRECT_PASSWORD, "hashed-password")).thenReturn(true);
        when(jwtTokenService.createAccessTokenForEmail(EMAIL)).thenReturn("JWT_TOKEN");
    }

    private void givenStoredAccount(boolean emailConfirmed, boolean lockedByAdmin) {
        UserAccount storedAccount = UserAccount.builder()
                .email(EMAIL)
                .passwordHash("hashed-password")
                .emailConfirmed(emailConfirmed)
                .lockedByAdmin(lockedByAdmin)
                .build();
        when(userAccountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(storedAccount));
    }

    @Test
    void confirmedAccountWithCorrectPasswordReceivesToken() {
        givenStoredAccount(true, false);

        String accessToken = userLoginService.loginAndIssueAccessToken(new LoginRequestDto(EMAIL, CORRECT_PASSWORD));

        assertEquals("JWT_TOKEN", accessToken);
    }

    @Test
    void unknownEmailIsRejectedWithoutCheckingPassword() {
        when(userAccountRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () ->
                userLoginService.loginAndIssueAccessToken(new LoginRequestDto("unknown@example.com", CORRECT_PASSWORD)));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenService, never()).createAccessTokenForEmail(anyString());
    }

    @Test
    void wrongPasswordIsRejected() {
        givenStoredAccount(true, false);

        assertThrows(InvalidCredentialsException.class, () ->
                userLoginService.loginAndIssueAccessToken(new LoginRequestDto(EMAIL, "wrong-password")));
        verify(jwtTokenService, never()).createAccessTokenForEmail(anyString());
    }

    @Test
    void wrongPasswordOnUnconfirmedAccountDoesNotRevealAccountStatus() {
        givenStoredAccount(false, true);

        assertThrows(InvalidCredentialsException.class, () ->
                userLoginService.loginAndIssueAccessToken(new LoginRequestDto(EMAIL, "wrong-password")));
    }

    @Test
    void unconfirmedEmailIsRejected() {
        givenStoredAccount(false, false);

        assertThrows(EmailNotConfirmedException.class, () ->
                userLoginService.loginAndIssueAccessToken(new LoginRequestDto(EMAIL, CORRECT_PASSWORD)));
        verify(jwtTokenService, never()).createAccessTokenForEmail(anyString());
    }

    @Test
    void accountLockedByAdminIsRejected() {
        givenStoredAccount(true, true);

        assertThrows(AccountLockedByAdminException.class, () ->
                userLoginService.loginAndIssueAccessToken(new LoginRequestDto(EMAIL, CORRECT_PASSWORD)));
        verify(jwtTokenService, never()).createAccessTokenForEmail(anyString());
    }
}

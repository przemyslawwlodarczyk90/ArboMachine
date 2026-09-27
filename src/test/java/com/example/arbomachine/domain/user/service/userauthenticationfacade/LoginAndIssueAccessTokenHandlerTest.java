package com.example.arbomachine.domain.user.service.userauthenticationfacade;

import com.example.arbomachine.domain.user.dto.LoginRequestDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.AccountLockedByAdminException;
import com.example.arbomachine.domain.user.exception.EmailNotConfirmedException;
import com.example.arbomachine.domain.user.exception.InvalidCredentialsException;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LoginAndIssueAccessTokenHandlerTest {

    private static final String EMAIL = "jan.kowalski@example.com";
    private static final String CORRECT_PASSWORD = "password123";

    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final LoginAndIssueAccessTokenHandler loginAndIssueAccessTokenHandler =
            new LoginAndIssueAccessTokenHandler(userAccountRepository, jwtTokenService, passwordEncoder);

    @BeforeEach
    void stubPasswordCheckAndTokenIssuing() {
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

        assertEquals("JWT_TOKEN", loginAndIssueAccessTokenHandler.handle(new LoginRequestDto(EMAIL, CORRECT_PASSWORD)));
    }

    @Test
    void unknownEmailIsRejectedWithoutCheckingPassword() {
        when(userAccountRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () ->
                loginAndIssueAccessTokenHandler.handle(new LoginRequestDto("unknown@example.com", CORRECT_PASSWORD)));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenService, never()).createAccessTokenForEmail(anyString());
    }

    @Test
    void wrongPasswordIsRejected() {
        givenStoredAccount(true, false);

        assertThrows(InvalidCredentialsException.class, () ->
                loginAndIssueAccessTokenHandler.handle(new LoginRequestDto(EMAIL, "wrong-password")));
        verify(jwtTokenService, never()).createAccessTokenForEmail(anyString());
    }

    @Test
    void wrongPasswordOnUnconfirmedAccountDoesNotRevealAccountStatus() {
        givenStoredAccount(false, true);

        assertThrows(InvalidCredentialsException.class, () ->
                loginAndIssueAccessTokenHandler.handle(new LoginRequestDto(EMAIL, "wrong-password")));
    }

    @Test
    void unconfirmedEmailIsRejected() {
        givenStoredAccount(false, false);

        assertThrows(EmailNotConfirmedException.class, () ->
                loginAndIssueAccessTokenHandler.handle(new LoginRequestDto(EMAIL, CORRECT_PASSWORD)));
        verify(jwtTokenService, never()).createAccessTokenForEmail(anyString());
    }

    @Test
    void accountLockedByAdminIsRejected() {
        givenStoredAccount(true, true);

        assertThrows(AccountLockedByAdminException.class, () ->
                loginAndIssueAccessTokenHandler.handle(new LoginRequestDto(EMAIL, CORRECT_PASSWORD)));
        verify(jwtTokenService, never()).createAccessTokenForEmail(anyString());
    }
}

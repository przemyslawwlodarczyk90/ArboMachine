package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.entity.PasswordResetToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.TokenExpiredException;
import com.example.arbomachine.domain.user.exception.TokenNotFoundException;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.service.EmailSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {

    private static final String EMAIL = "jan.kowalski@example.com";

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailSenderService emailSenderService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    private PasswordResetToken resetTokenExpiringAt(LocalDateTime expiresAt, UserAccount tokenOwnerAccount) {
        return PasswordResetToken.builder()
                .token("reset-token")
                .userAccount(tokenOwnerAccount)
                .createdAt(expiresAt.minusMinutes(30))
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    void activeAccountReceivesResetLinkAndOldTokensAreRemoved() {
        UserAccount confirmedAccount = UserAccount.builder().email(EMAIL).firstName("Jan").emailConfirmed(true).build();
        when(userAccountRepository.findByEmailAndEmailConfirmed(EMAIL, true)).thenReturn(Optional.of(confirmedAccount));

        passwordResetService.sendPasswordResetLinkIfAccountExists(EMAIL);

        ArgumentCaptor<PasswordResetToken> savedTokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).deleteByUserAccount(confirmedAccount);
        verify(passwordResetTokenRepository).save(savedTokenCaptor.capture());
        PasswordResetToken savedToken = savedTokenCaptor.getValue();
        assertEquals(confirmedAccount, savedToken.getUserAccount());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));
        verify(emailSenderService).sendPasswordResetEmail(eq(EMAIL), anyString(), eq("Jan"),
                contains("/reset-password?token=" + savedToken.getToken()), anyLong());
    }

    @Test
    void unknownOrUnconfirmedEmailSilentlyGetsNoLink() {
        when(userAccountRepository.findByEmailAndEmailConfirmed("nobody@example.com", true)).thenReturn(Optional.empty());

        passwordResetService.sendPasswordResetLinkIfAccountExists("nobody@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verifyNoInteractions(emailSenderService);
    }

    @Test
    void accountLockedByAdminSilentlyGetsNoLink() {
        UserAccount lockedAccount = UserAccount.builder().email(EMAIL).emailConfirmed(true).lockedByAdmin(true).build();
        when(userAccountRepository.findByEmailAndEmailConfirmed(EMAIL, true)).thenReturn(Optional.of(lockedAccount));

        passwordResetService.sendPasswordResetLinkIfAccountExists(EMAIL);

        verify(passwordResetTokenRepository, never()).save(any());
        verifyNoInteractions(emailSenderService);
    }

    @Test
    void validTokenSetsNewPasswordHashAndIsDeleted() {
        UserAccount tokenOwnerAccount = UserAccount.builder().email(EMAIL).passwordHash("old-hash").build();
        PasswordResetToken validToken = resetTokenExpiringAt(LocalDateTime.now().plusMinutes(10), tokenOwnerAccount);
        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode("newPassword1")).thenReturn("new-hash");

        passwordResetService.resetPasswordUsingToken("reset-token", "newPassword1");

        assertEquals("new-hash", tokenOwnerAccount.getPasswordHash());
        verify(userAccountRepository).save(tokenOwnerAccount);
        verify(passwordResetTokenRepository).delete(validToken);
    }

    @Test
    void expiredTokenCannotChangePassword() {
        PasswordResetToken expiredToken = resetTokenExpiringAt(LocalDateTime.now().minusHours(1), new UserAccount());
        when(passwordResetTokenRepository.findByToken("reset-token")).thenReturn(Optional.of(expiredToken));

        assertThrows(TokenExpiredException.class, () -> passwordResetService.resetPasswordUsingToken("reset-token", "newPassword1"));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void unknownTokenFailsValidation() {
        when(passwordResetTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(TokenNotFoundException.class, () -> passwordResetService.ensurePasswordResetTokenIsValid("missing"));
    }
}

package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.entity.EmailConfirmationToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.exception.EmailAlreadyInUseException;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.service.EmailSenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserRegistrationServiceTest {

    private static final String PLAIN_TEXT_PASSWORD = "password123";

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private EmailConfirmationTokenRepository emailConfirmationTokenRepository;

    @Spy
    private UserAccountMapper userAccountMapper = new UserAccountMapper();

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailSenderService emailSenderService;

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    private final UserProfileDto newUserProfile = UserProfileDto.builder()
            .firstName("Jan")
            .lastName("Kowalski")
            .email("jan.kowalski@example.com")
            .phoneNumber("123456789")
            .street("ul. Marszałkowska 123")
            .postalCode("00-001")
            .city("Warszawa")
            .build();

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.openMocks(this);
        when(passwordEncoder.encode(PLAIN_TEXT_PASSWORD)).thenReturn("hashed-password");
    }

    @Test
    void savesUnconfirmedAccountWithHashedPasswordAndChosenRole() {
        userRegistrationService.registerNewUserAccount(newUserProfile, PLAIN_TEXT_PASSWORD, AssignableUserRole.ARBORIST);

        ArgumentCaptor<UserAccount> savedAccountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(savedAccountCaptor.capture());
        UserAccount savedAccount = savedAccountCaptor.getValue();
        assertEquals("hashed-password", savedAccount.getPasswordHash());
        assertEquals(UserRole.ARBORIST, savedAccount.getRole());
        assertFalse(savedAccount.isEmailConfirmed());
    }

    @Test
    void sendsConfirmationEmailWithLinkContainingSavedToken() {
        userRegistrationService.registerNewUserAccount(newUserProfile, PLAIN_TEXT_PASSWORD, AssignableUserRole.OFFICIAL);

        ArgumentCaptor<EmailConfirmationToken> savedTokenCaptor = ArgumentCaptor.forClass(EmailConfirmationToken.class);
        verify(emailConfirmationTokenRepository).save(savedTokenCaptor.capture());
        EmailConfirmationToken savedToken = savedTokenCaptor.getValue();
        assertEquals(newUserProfile.email(), savedToken.getUserAccount().getEmail());

        verify(emailSenderService).sendRegistrationConfirmationEmail(eq(newUserProfile.email()),
                eq("Confirm your registration"), eq("Jan"), eq("Kowalski"),
                contains("/confirm?token=" + savedToken.getToken()));
    }

    @Test
    void registeringAgainWithUnconfirmedEmailReusesThatAccount() {
        UserAccount unconfirmedAccount = UserAccount.builder()
                .id(7L).email(newUserProfile.email()).firstName("Old").role(UserRole.OFFICIAL).build();
        when(userAccountRepository.findByEmail(newUserProfile.email())).thenReturn(Optional.of(unconfirmedAccount));

        userRegistrationService.registerNewUserAccount(newUserProfile, PLAIN_TEXT_PASSWORD, AssignableUserRole.ARBORIST);

        verify(userAccountRepository).save(unconfirmedAccount);
        assertEquals(7L, unconfirmedAccount.getId());
        assertEquals("Jan", unconfirmedAccount.getFirstName());
        assertEquals(UserRole.ARBORIST, unconfirmedAccount.getRole());
    }

    @Test
    void registeringWithEmailOfConfirmedAccountIsRejected() {
        UserAccount confirmedAccount = UserAccount.builder().email(newUserProfile.email()).emailConfirmed(true).build();
        when(userAccountRepository.findByEmail(newUserProfile.email())).thenReturn(Optional.of(confirmedAccount));

        assertThrows(EmailAlreadyInUseException.class, () ->
                userRegistrationService.registerNewUserAccount(newUserProfile, PLAIN_TEXT_PASSWORD, AssignableUserRole.ARBORIST));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void registeringWithEmailOfAccountLockedByAdminIsRejected() {
        UserAccount lockedUnconfirmedAccount = UserAccount.builder().email(newUserProfile.email()).lockedByAdmin(true).build();
        when(userAccountRepository.findByEmail(newUserProfile.email())).thenReturn(Optional.of(lockedUnconfirmedAccount));

        assertThrows(EmailAlreadyInUseException.class, () ->
                userRegistrationService.registerNewUserAccount(newUserProfile, PLAIN_TEXT_PASSWORD, AssignableUserRole.ARBORIST));
        verifyNoInteractions(emailSenderService);
    }
}

package com.example.arbomachine.domain.user.service.userauthenticationfacade;

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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RegisterNewUserAccountHandlerTest {

    private static final String PLAIN_TEXT_PASSWORD = "password123";

    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository = mock(EmailConfirmationTokenRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final EmailSenderService emailSenderService = mock(EmailSenderService.class);
    private final RegisterNewUserAccountHandler registerNewUserAccountHandler = new RegisterNewUserAccountHandler(
            userAccountRepository, emailConfirmationTokenRepository, new UserAccountMapper(), passwordEncoder, emailSenderService);

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
    void stubPasswordEncoder() {
        when(passwordEncoder.encode(PLAIN_TEXT_PASSWORD)).thenReturn("hashed-password");
    }

    @Test
    void savesUnconfirmedAccountWithHashedPasswordAndChosenRole() {
        registerNewUserAccountHandler.handle(newUserProfile, PLAIN_TEXT_PASSWORD, AssignableUserRole.ARBORIST);

        ArgumentCaptor<UserAccount> savedAccountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(savedAccountCaptor.capture());
        UserAccount savedAccount = savedAccountCaptor.getValue();
        assertEquals("hashed-password", savedAccount.getPasswordHash());
        assertEquals(UserRole.ARBORIST, savedAccount.getRole());
        assertFalse(savedAccount.isEmailConfirmed());
    }

    @Test
    void sendsConfirmationEmailWithLinkContainingSavedToken() {
        registerNewUserAccountHandler.handle(newUserProfile, PLAIN_TEXT_PASSWORD, AssignableUserRole.OFFICIAL);

        ArgumentCaptor<EmailConfirmationToken> savedTokenCaptor = ArgumentCaptor.forClass(EmailConfirmationToken.class);
        verify(emailConfirmationTokenRepository).save(savedTokenCaptor.capture());
        EmailConfirmationToken savedToken = savedTokenCaptor.getValue();
        assertEquals(newUserProfile.email(), savedToken.getUserAccount().getEmail());

        verify(emailSenderService).sendRegistrationConfirmationEmail(eq(newUserProfile.email()),
                eq("Potwierdź rejestrację w ArboMachine"), eq("Jan"), eq("Kowalski"),
                contains("/confirm?token=" + savedToken.getToken()));
    }

    @Test
    void registeringAgainWithUnconfirmedEmailReusesThatAccount() {
        UserAccount unconfirmedAccount = UserAccount.builder()
                .id(7L).email(newUserProfile.email()).firstName("Old").role(UserRole.OFFICIAL).build();
        when(userAccountRepository.findByEmail(newUserProfile.email())).thenReturn(Optional.of(unconfirmedAccount));

        registerNewUserAccountHandler.handle(newUserProfile, PLAIN_TEXT_PASSWORD, AssignableUserRole.ARBORIST);

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
                registerNewUserAccountHandler.handle(newUserProfile, PLAIN_TEXT_PASSWORD, AssignableUserRole.ARBORIST));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void registeringWithEmailOfAccountLockedByAdminIsRejected() {
        UserAccount lockedUnconfirmedAccount = UserAccount.builder().email(newUserProfile.email()).lockedByAdmin(true).build();
        when(userAccountRepository.findByEmail(newUserProfile.email())).thenReturn(Optional.of(lockedUnconfirmedAccount));

        assertThrows(EmailAlreadyInUseException.class, () ->
                registerNewUserAccountHandler.handle(newUserProfile, PLAIN_TEXT_PASSWORD, AssignableUserRole.ARBORIST));
        verifyNoInteractions(emailSenderService);
    }
}

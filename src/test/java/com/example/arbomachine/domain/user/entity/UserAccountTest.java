package com.example.arbomachine.domain.user.entity;

import com.example.arbomachine.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserAccountTest {

    private UserAccount confirmedArboristAccount;
    private EmailConfirmationToken existingConfirmationToken;

    @BeforeEach
    void createConfirmedArboristAccountWithOneToken() {
        existingConfirmationToken = EmailConfirmationToken.builder()
                .id(1L)
                .token("sample-token")
                .build();

        Set<EmailConfirmationToken> emailConfirmationTokens = new HashSet<>();
        emailConfirmationTokens.add(existingConfirmationToken);

        confirmedArboristAccount = UserAccount.builder()
                .id(1L)
                .firstName("Jan")
                .lastName("Kowalski")
                .passwordHash("hashed-password")
                .email("jan.kowalski@example.com")
                .phoneNumber("123456789")
                .street("ul. Marszałkowska 123")
                .postalCode("00-001")
                .city("Warszawa")
                .emailConfirmed(true)
                .role(UserRole.ARBORIST)
                .emailConfirmationTokens(emailConfirmationTokens)
                .build();
    }

    @Test
    void builderSetsAllFields() {
        assertEquals(1L, confirmedArboristAccount.getId());
        assertEquals("Jan", confirmedArboristAccount.getFirstName());
        assertEquals("Kowalski", confirmedArboristAccount.getLastName());
        assertEquals("hashed-password", confirmedArboristAccount.getPasswordHash());
        assertEquals("jan.kowalski@example.com", confirmedArboristAccount.getEmail());
        assertEquals("123456789", confirmedArboristAccount.getPhoneNumber());
        assertEquals("ul. Marszałkowska 123", confirmedArboristAccount.getStreet());
        assertEquals("00-001", confirmedArboristAccount.getPostalCode());
        assertEquals("Warszawa", confirmedArboristAccount.getCity());
        assertEquals(UserRole.ARBORIST, confirmedArboristAccount.getRole());
        assertTrue(confirmedArboristAccount.isEmailConfirmed());
        assertEquals(1, confirmedArboristAccount.getEmailConfirmationTokens().size());
    }

    @Test
    void newAccountIsNotLockedByDefault() {
        assertFalse(confirmedArboristAccount.isLockedByAdmin());
    }

    @Test
    void confirmationTokensCanBeAddedAndRemoved() {
        EmailConfirmationToken additionalConfirmationToken = EmailConfirmationToken.builder()
                .id(2L)
                .token("new-token")
                .build();

        confirmedArboristAccount.getEmailConfirmationTokens().add(additionalConfirmationToken);
        assertEquals(2, confirmedArboristAccount.getEmailConfirmationTokens().size());

        confirmedArboristAccount.getEmailConfirmationTokens().remove(existingConfirmationToken);
        confirmedArboristAccount.getEmailConfirmationTokens().remove(additionalConfirmationToken);
        assertEquals(0, confirmedArboristAccount.getEmailConfirmationTokens().size());
    }
}

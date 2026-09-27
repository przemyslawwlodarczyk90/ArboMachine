package com.example.arbomachine.domain.user.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EmailConfirmationTokenTest {

    private UserAccount tokenOwnerAccount;
    private EmailConfirmationToken emailConfirmationToken;

    @BeforeEach
    void createTokenForAccount() {
        tokenOwnerAccount = UserAccount.builder()
                .id(1L)
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan.kowalski@example.com")
                .build();

        emailConfirmationToken = EmailConfirmationToken.builder()
                .id(1L)
                .token("sample-token")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .userAccount(tokenOwnerAccount)
                .build();
    }

    @Test
    void builderSetsAllFields() {
        assertEquals(1L, emailConfirmationToken.getId());
        assertEquals("sample-token", emailConfirmationToken.getToken());
        assertNotNull(emailConfirmationToken.getCreatedAt());
        assertNotNull(emailConfirmationToken.getExpiresAt());
        assertEquals(tokenOwnerAccount, emailConfirmationToken.getUserAccount());
    }

    @Test
    void expirationDateCanBeChanged() {
        LocalDateTime laterExpirationDate = LocalDateTime.now().plusDays(3);

        emailConfirmationToken.setExpiresAt(laterExpirationDate);

        assertEquals(laterExpirationDate, emailConfirmationToken.getExpiresAt());
    }

    @Test
    void tokenCanBeReassignedToAnotherAccount() {
        UserAccount otherAccount = UserAccount.builder().id(2L).email("anna.nowak@example.com").build();

        emailConfirmationToken.setUserAccount(otherAccount);

        assertEquals(otherAccount, emailConfirmationToken.getUserAccount());
    }
}

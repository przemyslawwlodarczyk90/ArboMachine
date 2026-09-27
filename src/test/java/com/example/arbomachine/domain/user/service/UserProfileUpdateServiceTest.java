package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserProfileUpdateServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private UserProfileUpdateService userProfileUpdateService;

    private UserAccount storedAccount;

    @BeforeEach
    void initMocksAndStoredAccount() {
        MockitoAnnotations.openMocks(this);
        storedAccount = UserAccount.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com")
                .phoneNumber("123456789").street("Street 1").postalCode("00-001").city("City")
                .emailConfirmed(true)
                .build();
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(storedAccount));
    }

    @Test
    void overwritesProfileFieldsButKeepsEmail() {
        UserProfileDto updatedProfile = new UserProfileDto(1L, "John", "Doe", "john.doe@example.com",
                "987654321", "New Street", "00-002", "New City");

        userProfileUpdateService.updateUserProfile(1L, updatedProfile);

        verify(userAccountRepository).save(storedAccount);
        assertEquals("987654321", storedAccount.getPhoneNumber());
        assertEquals("New Street", storedAccount.getStreet());
        assertEquals("00-002", storedAccount.getPostalCode());
        assertEquals("New City", storedAccount.getCity());
        assertEquals("john.doe@example.com", storedAccount.getEmail());
    }

    @Test
    void changingEmailIsRejected() {
        UserProfileDto profileWithDifferentEmail = new UserProfileDto(1L, "John", "Doe", "new.email@example.com",
                "123456789", "Street 1", "00-001", "City");

        assertThrows(IllegalArgumentException.class, () -> userProfileUpdateService.updateUserProfile(1L, profileWithDifferentEmail));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void missingAccountIsReported() {
        when(userAccountRepository.findById(99L)).thenReturn(Optional.empty());
        UserProfileDto anyProfile = new UserProfileDto(99L, "A", "B", "a@example.com", null, null, null, null);

        assertThrows(UserAccountNotFoundException.class, () -> userProfileUpdateService.updateUserProfile(99L, anyProfile));
    }
}

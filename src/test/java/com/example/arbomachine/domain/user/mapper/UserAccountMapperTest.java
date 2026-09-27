package com.example.arbomachine.domain.user.mapper;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserAccountMapperTest {

    private final UserAccountMapper userAccountMapper = new UserAccountMapper();

    private UserAccount lockedOfficialAccount() {
        return UserAccount.builder()
                .id(1L)
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan.kowalski@example.com")
                .phoneNumber("123456789")
                .street("ul. Marszałkowska 123")
                .postalCode("00-001")
                .city("Warszawa")
                .role(UserRole.OFFICIAL)
                .emailConfirmed(true)
                .lockedByAdmin(true)
                .passwordHash("hashed-password")
                .build();
    }

    @Test
    void mapsEntityToProfileDto() {
        UserAccount userAccount = lockedOfficialAccount();

        UserProfileDto userProfile = userAccountMapper.toUserProfileDto(userAccount);

        assertEquals(userAccount.getId(), userProfile.id());
        assertEquals(userAccount.getFirstName(), userProfile.firstName());
        assertEquals(userAccount.getLastName(), userProfile.lastName());
        assertEquals(userAccount.getEmail(), userProfile.email());
        assertEquals(userAccount.getPhoneNumber(), userProfile.phoneNumber());
        assertEquals(userAccount.getStreet(), userProfile.street());
        assertEquals(userAccount.getPostalCode(), userProfile.postalCode());
        assertEquals(userAccount.getCity(), userProfile.city());
    }

    @Test
    void mapsEntityToAccountDtoIncludingRoleAndStatusFlags() {
        UserAccountDto userAccountDto = userAccountMapper.toUserAccountDto(lockedOfficialAccount());

        assertEquals(UserRole.OFFICIAL, userAccountDto.role());
        assertTrue(userAccountDto.emailConfirmed());
        assertTrue(userAccountDto.lockedByAdmin());
    }

    @Test
    void createsNewEntityFromProfileWithoutPasswordOrRole() {
        UserProfileDto userProfile = UserProfileDto.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan.kowalski@example.com")
                .phoneNumber("123456789")
                .street("ul. Marszałkowska 123")
                .postalCode("00-001")
                .city("Warszawa")
                .build();

        UserAccount newUserAccount = userAccountMapper.toNewUserAccountEntity(userProfile);

        assertEquals(userProfile.firstName(), newUserAccount.getFirstName());
        assertEquals(userProfile.lastName(), newUserAccount.getLastName());
        assertEquals(userProfile.email(), newUserAccount.getEmail());
        assertEquals(userProfile.phoneNumber(), newUserAccount.getPhoneNumber());
        assertEquals(userProfile.street(), newUserAccount.getStreet());
        assertEquals(userProfile.postalCode(), newUserAccount.getPostalCode());
        assertEquals(userProfile.city(), newUserAccount.getCity());
        // Haslo, role i flagi ustawia dopiero serwis rejestracji.
        assertNull(newUserAccount.getPasswordHash());
        assertNull(newUserAccount.getRole());
        assertFalse(newUserAccount.isEmailConfirmed());
    }
}

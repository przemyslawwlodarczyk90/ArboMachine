package com.example.arbomachine.domain.user.mapper;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import org.springframework.stereotype.Component;

/** Przepisywanie danych miedzy encja UserAccount a DTO wystawianymi przez API. */
@Component
public class UserAccountMapper {

    public UserProfileDto toUserProfileDto(UserAccount userAccount) {
        return new UserProfileDto(
                userAccount.getId(),
                userAccount.getFirstName(),
                userAccount.getLastName(),
                userAccount.getEmail(),
                userAccount.getPhoneNumber(),
                userAccount.getStreet(),
                userAccount.getPostalCode(),
                userAccount.getCity()
        );
    }

    public UserAccountDto toUserAccountDto(UserAccount userAccount) {
        return new UserAccountDto(
                userAccount.getId(),
                userAccount.getFirstName(),
                userAccount.getLastName(),
                userAccount.getEmail(),
                userAccount.getPhoneNumber(),
                userAccount.getStreet(),
                userAccount.getPostalCode(),
                userAccount.getCity(),
                userAccount.getRole(),
                userAccount.isEmailConfirmed(),
                userAccount.isLockedByAdmin()
        );
    }

    /** Nowa, jeszcze niezapisana encja z danymi profilu - haslo, role i flagi ustawia serwis rejestracji. */
    public UserAccount toNewUserAccountEntity(UserProfileDto userProfile) {
        return UserAccount.builder()
                .id(userProfile.id())
                .firstName(userProfile.firstName())
                .lastName(userProfile.lastName())
                .email(userProfile.email())
                .phoneNumber(userProfile.phoneNumber())
                .street(userProfile.street())
                .postalCode(userProfile.postalCode())
                .city(userProfile.city())
                .build();
    }
}

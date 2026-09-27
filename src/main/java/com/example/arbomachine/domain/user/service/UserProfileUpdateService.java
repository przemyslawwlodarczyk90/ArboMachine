package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

/**
 * Zapis zmian w danych profilu. Nie sprawdza uprawnien - robia to wywolujacy
 * (fasada dla wlasnego konta, serwis admina dla cudzych kont).
 */
@Service
public class UserProfileUpdateService {
    private final UserAccountRepository userAccountRepository;

    public UserProfileUpdateService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /** Nadpisuje dane profilu. E-mail jest identyfikatorem logowania, wiec nie wolno go zmienic. */
    public void updateUserProfile(Long userAccountId, UserProfileDto updatedProfile) {
        UserAccount userAccount = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));

        if (!updatedProfile.email().equals(userAccount.getEmail())) {
            throw new IllegalArgumentException("Email cannot be changed");
        }

        userAccount.setFirstName(updatedProfile.firstName());
        userAccount.setLastName(updatedProfile.lastName());
        userAccount.setPhoneNumber(updatedProfile.phoneNumber());
        userAccount.setStreet(updatedProfile.street());
        userAccount.setPostalCode(updatedProfile.postalCode());
        userAccount.setCity(updatedProfile.city());
        userAccountRepository.save(userAccount);
    }
}

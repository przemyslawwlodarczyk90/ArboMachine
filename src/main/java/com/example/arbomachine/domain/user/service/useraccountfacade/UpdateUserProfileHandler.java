package com.example.arbomachine.domain.user.service.useraccountfacade;

import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/**
 * Zapis zmian w danych profilu konta o podanym id. Wspolny handler: uzywany przy edycji wlasnego profilu
 * i przez admina. Nie sprawdza uprawnien - robia to wywolujace handlery.
 */
@Component
public class UpdateUserProfileHandler {
    private final UserAccountRepository userAccountRepository;

    public UpdateUserProfileHandler(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /** Nadpisuje dane profilu. E-mail jest identyfikatorem logowania, wiec nie wolno go zmienic. */
    public void handle(Long userAccountId, UserProfileDto updatedProfile) {
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

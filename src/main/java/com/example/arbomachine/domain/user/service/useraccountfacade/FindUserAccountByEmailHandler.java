package com.example.arbomachine.domain.user.service.useraccountfacade;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/**
 * Konto po adresie e-mail (e-mail jest nazwa uzytkownika w tokenie JWT).
 * Wspolny handler: uzywany przez handlery tej fasady oraz - jako waski kontrakt - przez moduly projektow i drzew
 * do ustalenia wlasciciela na podstawie zalogowanego uzytkownika.
 */
@Component
public class FindUserAccountByEmailHandler {
    private final UserAccountRepository userAccountRepository;

    public FindUserAccountByEmailHandler(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount handle(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));
    }
}

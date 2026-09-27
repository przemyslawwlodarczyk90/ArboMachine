package com.example.arbomachine.domain.user.service.adminuseraccountfacade;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.exception.AdminAccountModificationForbiddenException;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/**
 * Wspolny handler operacji admina: konto o podanym id, ktore admin MOZE zmieniac.
 * Konta administratorow zarzadzane sa wylacznie w bazie, wiec dla roli ADMIN rzuca wyjatek (403).
 */
@Component
public class FindUserAccountManageableByAdminHandler {
    private final UserAccountRepository userAccountRepository;

    public FindUserAccountManageableByAdminHandler(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount handle(Long userAccountId) {
        UserAccount userAccount = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));
        if (userAccount.getRole() == UserRole.ADMIN) {
            throw new AdminAccountModificationForbiddenException(
                    "Admin accounts can only be managed directly in the database");
        }
        return userAccount;
    }
}

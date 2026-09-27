package com.example.arbomachine.domain.user.service.useraccountfacade;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Trwale usuwa konto razem z jego tokenami. Wspolny handler: uzywany przy usuwaniu wlasnego konta i przez admina.
 * Nie sprawdza uprawnien - robia to wywolujace handlery.
 * <p>
 * Przed usunieciem pyta wszystkie {@link UserAccountDeletionGuard} z innych modulow (np. projektow),
 * czy konto nie jest jeszcze wlascicielem ich danych.
 */
@Component
public class DeleteUserAccountByIdHandler {
    private final UserAccountRepository userAccountRepository;
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final List<UserAccountDeletionGuard> userAccountDeletionGuards;

    public DeleteUserAccountByIdHandler(UserAccountRepository userAccountRepository,
                                        EmailConfirmationTokenRepository emailConfirmationTokenRepository,
                                        PasswordResetTokenRepository passwordResetTokenRepository,
                                        List<UserAccountDeletionGuard> userAccountDeletionGuards) {
        this.userAccountRepository = userAccountRepository;
        this.emailConfirmationTokenRepository = emailConfirmationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userAccountDeletionGuards = userAccountDeletionGuards;
    }

    @Transactional
    public void handle(Long userAccountId) {
        UserAccount userAccount = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));
        userAccountDeletionGuards.forEach(deletionGuard -> deletionGuard.ensureUserAccountCanBeDeleted(userAccount));
        // Tokeny maja klucz obcy do konta, wiec musza zniknac pierwsze.
        emailConfirmationTokenRepository.deleteByUserAccount(userAccount);
        passwordResetTokenRepository.deleteByUserAccount(userAccount);
        userAccountRepository.delete(userAccount);
    }
}

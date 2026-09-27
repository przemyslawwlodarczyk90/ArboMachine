package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Trwale usuwa konto razem z jego tokenami. Nie sprawdza uprawnien - robia to wywolujacy
 * (fasada dla wlasnego konta, serwis admina dla cudzych kont).
 * <p>
 * Przed usunieciem pyta wszystkie {@link UserAccountDeletionGuard} z innych modulow (np. projektow),
 * czy konto nie jest jeszcze wlascicielem ich danych.
 */
@Service
public class UserAccountDeletionService {
    private final UserAccountRepository userAccountRepository;
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final List<UserAccountDeletionGuard> userAccountDeletionGuards;

    public UserAccountDeletionService(UserAccountRepository userAccountRepository,
                                      EmailConfirmationTokenRepository emailConfirmationTokenRepository,
                                      PasswordResetTokenRepository passwordResetTokenRepository,
                                      List<UserAccountDeletionGuard> userAccountDeletionGuards) {
        this.userAccountRepository = userAccountRepository;
        this.emailConfirmationTokenRepository = emailConfirmationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.userAccountDeletionGuards = userAccountDeletionGuards;
    }

    @Transactional
    public void deleteUserAccountById(Long userAccountId) {
        UserAccount userAccount = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));
        userAccountDeletionGuards.forEach(deletionGuard -> deletionGuard.ensureUserAccountCanBeDeleted(userAccount));
        // Tokeny maja klucz obcy do konta, wiec musza zniknac pierwsze.
        emailConfirmationTokenRepository.deleteByUserAccount(userAccount);
        passwordResetTokenRepository.deleteByUserAccount(userAccount);
        userAccountRepository.delete(userAccount);
    }
}

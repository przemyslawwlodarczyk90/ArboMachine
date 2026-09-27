package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.PasswordResetTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Trwale usuwa konto razem z jego tokenami. Nie sprawdza uprawnien - robia to wywolujacy
 * (fasada dla wlasnego konta, serwis admina dla cudzych kont).
 */
@Service
public class UserAccountDeletionService {
    private final UserAccountRepository userAccountRepository;
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public UserAccountDeletionService(UserAccountRepository userAccountRepository,
                                      EmailConfirmationTokenRepository emailConfirmationTokenRepository,
                                      PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userAccountRepository = userAccountRepository;
        this.emailConfirmationTokenRepository = emailConfirmationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Transactional
    public void deleteUserAccountById(Long userAccountId) {
        UserAccount userAccount = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));
        // Tokeny maja klucz obcy do konta, wiec musza zniknac pierwsze.
        emailConfirmationTokenRepository.deleteByUserAccount(userAccount);
        passwordResetTokenRepository.deleteByUserAccount(userAccount);
        userAccountRepository.delete(userAccount);
    }
}

package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.user.entity.EmailConfirmationToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EmailConfirmationTokenRepositoryTest {

    @Autowired
    private EmailConfirmationTokenRepository emailConfirmationTokenRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount saveTokenOwnerAccount() {
        return userAccountRepository.save(UserAccount.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan.kowalski@example.com")
                .passwordHash("hashed-password")
                .build());
    }

    private void saveTokenFor(UserAccount tokenOwnerAccount) {
        emailConfirmationTokenRepository.save(EmailConfirmationToken.builder()
                .token("test-token")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .userAccount(tokenOwnerAccount)
                .build());
    }

    @Test
    void findsTokenTogetherWithItsAccount() {
        saveTokenFor(saveTokenOwnerAccount());

        assertThat(emailConfirmationTokenRepository.findByToken("test-token"))
                .hasValueSatisfying(foundToken ->
                        assertThat(foundToken.getUserAccount().getEmail()).isEqualTo("jan.kowalski@example.com"));
        assertThat(emailConfirmationTokenRepository.findByToken("non-existent-token")).isEmpty();
    }

    @Test
    void deletesAllTokensOfAnAccount() {
        UserAccount tokenOwnerAccount = saveTokenOwnerAccount();
        saveTokenFor(tokenOwnerAccount);

        emailConfirmationTokenRepository.deleteByUserAccount(tokenOwnerAccount);

        assertThat(emailConfirmationTokenRepository.findByToken("test-token")).isEmpty();
    }
}

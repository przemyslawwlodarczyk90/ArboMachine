package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Zapytania pochodne Spring Data na prawdziwej (wbudowanej H2) bazie. */
@DataJpaTest
class UserAccountRepositoryTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount saveAccount(String email, UserRole role, boolean emailConfirmed, boolean lockedByAdmin) {
        return userAccountRepository.save(UserAccount.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email(email)
                .passwordHash("hashed-password")
                .role(role)
                .emailConfirmed(emailConfirmed)
                .lockedByAdmin(lockedByAdmin)
                .build());
    }

    @Test
    void findsAccountByEmail() {
        saveAccount("jan.kowalski@example.com", UserRole.ARBORIST, true, false);

        assertThat(userAccountRepository.findByEmail("jan.kowalski@example.com"))
                .hasValueSatisfying(foundAccount -> assertThat(foundAccount.getFirstName()).isEqualTo("Jan"));
        assertThat(userAccountRepository.findByEmail("nonexistent@example.com")).isEmpty();
    }

    @Test
    void findsOnlyConfirmedAccountWhenAskedForConfirmedOne() {
        saveAccount("unconfirmed@example.com", UserRole.OFFICIAL, false, false);

        assertThat(userAccountRepository.findByEmailAndEmailConfirmed("unconfirmed@example.com", true)).isEmpty();
        assertThat(userAccountRepository.findByEmailAndEmailConfirmed("unconfirmed@example.com", false)).isPresent();
    }

    @Test
    void countsAccountsByRoleAndStatus() {
        saveAccount("a1@example.com", UserRole.ARBORIST, true, false);
        saveAccount("a2@example.com", UserRole.ARBORIST, false, true);
        saveAccount("o1@example.com", UserRole.OFFICIAL, true, false);

        assertThat(userAccountRepository.countByRole(UserRole.ARBORIST)).isEqualTo(2);
        assertThat(userAccountRepository.findByRole(UserRole.OFFICIAL)).hasSize(1);
        assertThat(userAccountRepository.countByEmailConfirmed(false)).isEqualTo(1);
        assertThat(userAccountRepository.countByLockedByAdmin(true)).isEqualTo(1);
    }
}

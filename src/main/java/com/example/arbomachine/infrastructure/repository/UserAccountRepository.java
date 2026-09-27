package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByEmailAndEmailConfirmed(String email, boolean emailConfirmed);

    List<UserAccount> findByRole(UserRole role);

    long countByRole(UserRole role);

    long countByEmailConfirmed(boolean emailConfirmed);

    long countByLockedByAdmin(boolean lockedByAdmin);
}

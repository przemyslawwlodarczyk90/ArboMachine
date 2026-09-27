package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.user.entity.PasswordResetToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUserAccount(UserAccount userAccount);
}

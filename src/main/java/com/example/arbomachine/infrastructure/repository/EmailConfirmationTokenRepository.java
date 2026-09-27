package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.user.entity.EmailConfirmationToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailConfirmationTokenRepository extends JpaRepository<EmailConfirmationToken, Long> {

    Optional<EmailConfirmationToken> findByToken(String token);

    void deleteByUserAccount(UserAccount userAccount);
}

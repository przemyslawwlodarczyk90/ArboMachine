package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.notification.entity.UserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Powiadomienia zawsze w kontekscie konta zalogowanego uzytkownika (albo odbiorcy z regul alertow). */
@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findAllByUserAccountIdOrderByCreatedAtDescIdDesc(Long userAccountId, Pageable pageable);

    boolean existsByUserAccountIdAndDeduplicationKey(Long userAccountId, String deduplicationKey);

    @Modifying
    @Query("delete from UserNotification notification where notification.userAccount.id = :userAccountId")
    void deleteAllOfUserAccount(@Param("userAccountId") Long userAccountId);
}

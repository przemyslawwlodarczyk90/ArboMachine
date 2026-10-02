package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.notification.entity.WebPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Subskrypcje Web Push - endpoint unikalny w calej bazie (jedna przegladarka = jedna subskrypcja). */
@Repository
public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {

    Optional<WebPushSubscription> findByEndpoint(String endpoint);

    List<WebPushSubscription> findAllByUserAccountId(Long userAccountId);

    Optional<WebPushSubscription> findByEndpointAndUserAccountId(String endpoint, Long userAccountId);

    @Modifying
    @Query("delete from WebPushSubscription subscription where subscription.userAccount.id = :userAccountId")
    void deleteAllOfUserAccount(@Param("userAccountId") Long userAccountId);
}

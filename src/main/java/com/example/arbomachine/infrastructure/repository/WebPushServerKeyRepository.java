package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.notification.entity.WebPushServerKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Klucze VAPID serwera - w praktyce jeden wiersz (najstarszy wygrywa, gdyby dwa procesy wygenerowaly naraz). */
@Repository
public interface WebPushServerKeyRepository extends JpaRepository<WebPushServerKey, Long> {

    Optional<WebPushServerKey> findFirstByOrderByIdAsc();
}

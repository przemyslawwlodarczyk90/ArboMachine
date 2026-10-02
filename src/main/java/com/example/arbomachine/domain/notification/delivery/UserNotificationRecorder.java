package com.example.arbomachine.domain.notification.delivery;

import com.example.arbomachine.domain.notification.entity.UserNotification;
import com.example.arbomachine.domain.notification.entity.UserNotificationType;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.repository.UserNotificationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Zapis powiadomienia z deduplikacja. Wlasna transakcja (REQUIRES_NEW): wywolywany takze z nasluchu zdarzen PO
 * zatwierdzeniu transakcji zrodlowej, gdzie zwykle REQUIRED dolaczyloby do juz zakonczonej transakcji.
 */
@Component
public class UserNotificationRecorder {
    private final UserNotificationRepository userNotificationRepository;
    private final UserAccountRepository userAccountRepository;

    public UserNotificationRecorder(UserNotificationRepository userNotificationRepository,
                                    UserAccountRepository userAccountRepository) {
        this.userNotificationRepository = userNotificationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /** Pusty wynik = takie powiadomienie juz istnieje (ten sam deduplicationKey) albo konto nie istnieje. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<UserNotification> record(long userAccountId, UserNotificationType type, String title, String body,
                                             String linkPath, String deduplicationKey) {
        if (userNotificationRepository.existsByUserAccountIdAndDeduplicationKey(userAccountId, deduplicationKey)) {
            return Optional.empty();
        }
        // Rownolegly duplikat (ten sam klucz w tej samej chwili) konczy sie naruszeniem unikalnosci - obsluguje go
        // NotificationDispatcher (wyjatek po wycofaniu tej transakcji).
        return userAccountRepository.findById(userAccountId).map(userAccount -> userNotificationRepository.saveAndFlush(
                UserNotification.create(userAccount, type, title, body, linkPath, deduplicationKey,
                        Instant.now().truncatedTo(ChronoUnit.MICROS))));
    }
}

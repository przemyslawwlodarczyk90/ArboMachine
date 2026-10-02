package com.example.arbomachine.domain.notification.delivery;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.useraccountfacade.UserAccountDataCleaner;
import com.example.arbomachine.infrastructure.repository.UserNotificationRepository;
import com.example.arbomachine.infrastructure.repository.WebPushSubscriptionRepository;
import org.springframework.stereotype.Component;

/** Usuwane konto traci powiadomienia i subskrypcje push (wywolywane w transakcji usuwania konta). */
@Component
public class NotificationUserAccountDataCleaner implements UserAccountDataCleaner {
    private final UserNotificationRepository userNotificationRepository;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;

    public NotificationUserAccountDataCleaner(UserNotificationRepository userNotificationRepository,
                                              WebPushSubscriptionRepository webPushSubscriptionRepository) {
        this.userNotificationRepository = userNotificationRepository;
        this.webPushSubscriptionRepository = webPushSubscriptionRepository;
    }

    @Override
    public void deleteDataOf(UserAccount userAccountToDelete) {
        userNotificationRepository.deleteAllOfUserAccount(userAccountToDelete.getId());
        webPushSubscriptionRepository.deleteAllOfUserAccount(userAccountToDelete.getId());
    }
}

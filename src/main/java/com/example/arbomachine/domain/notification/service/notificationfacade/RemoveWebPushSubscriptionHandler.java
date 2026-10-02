package com.example.arbomachine.domain.notification.service.notificationfacade;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.useraccountfacade.FindUserAccountByEmailHandler;
import com.example.arbomachine.infrastructure.repository.WebPushSubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wylaczenie push na tym urzadzeniu - usuwa subskrypcje TYLKO wlasnego konta (cudzy endpoint: nic sie nie dzieje).
 * Idempotentne.
 */
@Component
public class RemoveWebPushSubscriptionHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;

    public RemoveWebPushSubscriptionHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                            WebPushSubscriptionRepository webPushSubscriptionRepository) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.webPushSubscriptionRepository = webPushSubscriptionRepository;
    }

    @Transactional
    public void handle(String currentUserEmail, String endpoint) {
        UserAccount currentUser = findUserAccountByEmailHandler.handle(currentUserEmail);
        webPushSubscriptionRepository.findByEndpointAndUserAccountId(endpoint.trim(), currentUser.getId())
                .ifPresent(webPushSubscriptionRepository::delete);
    }
}

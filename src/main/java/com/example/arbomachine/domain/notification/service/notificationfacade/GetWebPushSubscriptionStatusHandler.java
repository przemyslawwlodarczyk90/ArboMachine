package com.example.arbomachine.domain.notification.service.notificationfacade;

import com.example.arbomachine.domain.notification.dto.WebPushSubscriptionStatusResponse;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.useraccountfacade.FindUserAccountByEmailHandler;
import com.example.arbomachine.infrastructure.repository.WebPushSubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Czy subskrypcja tej przegladarki jest zapisana dla zalogowanego konta (nie zdradza, czy nalezy do innego). */
@Component
public class GetWebPushSubscriptionStatusHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;

    public GetWebPushSubscriptionStatusHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                               WebPushSubscriptionRepository webPushSubscriptionRepository) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.webPushSubscriptionRepository = webPushSubscriptionRepository;
    }

    @Transactional(readOnly = true)
    public WebPushSubscriptionStatusResponse handle(String currentUserEmail, String endpoint) {
        UserAccount currentUser = findUserAccountByEmailHandler.handle(currentUserEmail);
        return new WebPushSubscriptionStatusResponse(
                webPushSubscriptionRepository.findByEndpointAndUserAccountId(endpoint.trim(), currentUser.getId()).isPresent());
    }
}

package com.example.arbomachine.domain.notification.facade;

import com.example.arbomachine.domain.notification.dto.TestNotificationResponse;
import com.example.arbomachine.domain.notification.dto.UserNotificationResponse;
import com.example.arbomachine.domain.notification.dto.WebPushConfigResponse;
import com.example.arbomachine.domain.notification.dto.WebPushSubscriptionRequest;
import com.example.arbomachine.domain.notification.dto.WebPushSubscriptionStatusResponse;
import com.example.arbomachine.domain.notification.service.notificationfacade.GetNotificationsOfCurrentUserHandler;
import com.example.arbomachine.domain.notification.service.notificationfacade.GetWebPushConfigHandler;
import com.example.arbomachine.domain.notification.service.notificationfacade.GetWebPushSubscriptionStatusHandler;
import com.example.arbomachine.domain.notification.service.notificationfacade.RemoveWebPushSubscriptionHandler;
import com.example.arbomachine.domain.notification.service.notificationfacade.SaveWebPushSubscriptionHandler;
import com.example.arbomachine.domain.notification.service.notificationfacade.SendTestNotificationHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Powiadomienia zalogowanego uzytkownika (etapy 29-30): lista powiadomien i subskrypcje Web Push jego urzadzen.
 * Reguly tworzace powiadomienia (alert stanu drzewa, przypomnienia o pracach) sa w pakietach delivery i reminder.
 */
@Component
public class NotificationFacade {
    private final GetNotificationsOfCurrentUserHandler getNotificationsOfCurrentUserHandler;
    private final GetWebPushConfigHandler getWebPushConfigHandler;
    private final SaveWebPushSubscriptionHandler saveWebPushSubscriptionHandler;
    private final RemoveWebPushSubscriptionHandler removeWebPushSubscriptionHandler;
    private final GetWebPushSubscriptionStatusHandler getWebPushSubscriptionStatusHandler;
    private final SendTestNotificationHandler sendTestNotificationHandler;

    public NotificationFacade(GetNotificationsOfCurrentUserHandler getNotificationsOfCurrentUserHandler,
                              GetWebPushConfigHandler getWebPushConfigHandler,
                              SaveWebPushSubscriptionHandler saveWebPushSubscriptionHandler,
                              RemoveWebPushSubscriptionHandler removeWebPushSubscriptionHandler,
                              GetWebPushSubscriptionStatusHandler getWebPushSubscriptionStatusHandler,
                              SendTestNotificationHandler sendTestNotificationHandler) {
        this.getNotificationsOfCurrentUserHandler = getNotificationsOfCurrentUserHandler;
        this.getWebPushConfigHandler = getWebPushConfigHandler;
        this.saveWebPushSubscriptionHandler = saveWebPushSubscriptionHandler;
        this.removeWebPushSubscriptionHandler = removeWebPushSubscriptionHandler;
        this.getWebPushSubscriptionStatusHandler = getWebPushSubscriptionStatusHandler;
        this.sendTestNotificationHandler = sendTestNotificationHandler;
    }

    public List<UserNotificationResponse> getNotificationsOfCurrentUser(String currentUserEmail) {
        return getNotificationsOfCurrentUserHandler.handle(currentUserEmail);
    }

    public WebPushConfigResponse getWebPushConfig() {
        return getWebPushConfigHandler.handle();
    }

    public void saveWebPushSubscriptionOfCurrentUser(String currentUserEmail, WebPushSubscriptionRequest request) {
        saveWebPushSubscriptionHandler.handle(currentUserEmail, request);
    }

    public void removeWebPushSubscriptionOfCurrentUser(String currentUserEmail, String endpoint) {
        removeWebPushSubscriptionHandler.handle(currentUserEmail, endpoint);
    }

    public WebPushSubscriptionStatusResponse getWebPushSubscriptionStatusOfCurrentUser(String currentUserEmail, String endpoint) {
        return getWebPushSubscriptionStatusHandler.handle(currentUserEmail, endpoint);
    }

    public TestNotificationResponse sendTestNotificationToCurrentUser(String currentUserEmail) {
        return sendTestNotificationHandler.handle(currentUserEmail);
    }
}

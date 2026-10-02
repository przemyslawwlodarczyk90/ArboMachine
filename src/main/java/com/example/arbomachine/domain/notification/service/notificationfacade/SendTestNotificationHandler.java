package com.example.arbomachine.domain.notification.service.notificationfacade;

import com.example.arbomachine.domain.notification.dto.TestNotificationResponse;
import com.example.arbomachine.domain.notification.entity.UserNotification;
import com.example.arbomachine.domain.notification.entity.UserNotificationType;
import com.example.arbomachine.domain.notification.push.WebPushDelivery;
import com.example.arbomachine.domain.notification.delivery.NotificationDispatcher;
import com.example.arbomachine.domain.notification.delivery.UserNotificationRecorder;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.useraccountfacade.FindUserAccountByEmailHandler;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * "Wyslij powiadomienie testowe": zapis powiadomienia TEST i wysylka push od razu (nie w tle), zeby uzytkownik
 * dostal odpowiedz, na ile urzadzen dotarlo.
 */
@Component
public class SendTestNotificationHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final UserNotificationRecorder userNotificationRecorder;
    private final WebPushDelivery webPushDelivery;

    public SendTestNotificationHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                       UserNotificationRecorder userNotificationRecorder, WebPushDelivery webPushDelivery) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.userNotificationRecorder = userNotificationRecorder;
        this.webPushDelivery = webPushDelivery;
    }

    public TestNotificationResponse handle(String currentUserEmail) {
        UserAccount currentUser = findUserAccountByEmailHandler.handle(currentUserEmail);
        UserNotification notification = userNotificationRecorder.record(currentUser.getId(), UserNotificationType.TEST,
                        "Powiadomienie testowe ArboMachine", "Powiadomienia push działają na tym urządzeniu.",
                        null, "test:" + UUID.randomUUID())
                .orElseThrow();
        return new TestNotificationResponse(
                webPushDelivery.deliverToAllDevicesOf(currentUser.getId(), NotificationDispatcher.messageOf(notification)));
    }
}

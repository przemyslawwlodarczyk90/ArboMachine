package com.example.arbomachine.domain.notification.delivery;

import com.example.arbomachine.domain.notification.entity.UserNotification;
import com.example.arbomachine.domain.notification.entity.UserNotificationType;
import com.example.arbomachine.domain.notification.push.WebPushDelivery;
import com.example.arbomachine.domain.notification.push.WebPushMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Jedno wejscie dla regul powiadomien: zapis powiadomienia (lista w aplikacji, deduplikacja) i - w tle - Web Push
 * na urzadzenia uzytkownika. Push nigdy nie blokuje ani nie psuje operacji, ktora wywolala powiadomienie.
 * Punkt rozszerzenia na kolejne kanaly (e-mail, SMS - DO_DOPRECYZOWANIA).
 */
@Component
public class NotificationDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final UserNotificationRecorder userNotificationRecorder;
    private final WebPushDelivery webPushDelivery;
    private final Executor notificationDeliveryExecutor;

    public NotificationDispatcher(UserNotificationRecorder userNotificationRecorder, WebPushDelivery webPushDelivery,
                                  @Qualifier(NotificationDeliveryConfig.EXECUTOR_BEAN_NAME) Executor notificationDeliveryExecutor) {
        this.userNotificationRecorder = userNotificationRecorder;
        this.webPushDelivery = webPushDelivery;
        this.notificationDeliveryExecutor = notificationDeliveryExecutor;
    }

    /** Zapisuje powiadomienie (o ile nie ma duplikatu) i zleca wysylke push w tle. Zwraca zapisane powiadomienie. */
    public Optional<UserNotification> notifyUser(long userAccountId, UserNotificationType type, String title, String body,
                                                 String linkPath, String deduplicationKey) {
        Optional<UserNotification> recorded;
        try {
            recorded = userNotificationRecorder.record(userAccountId, type, title, body, linkPath, deduplicationKey);
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            return Optional.empty();
        }
        recorded.ifPresent(notification -> {
            WebPushMessage message = messageOf(notification);
            notificationDeliveryExecutor.execute(() -> {
                try {
                    webPushDelivery.deliverToAllDevicesOf(userAccountId, message);
                } catch (RuntimeException deliveryError) {
                    LOGGER.warn("Web Push delivery of notification {} failed: {}", notification.getId(), deliveryError.toString());
                }
            });
        });
        return recorded;
    }

    public static WebPushMessage messageOf(UserNotification notification) {
        return new WebPushMessage(notification.getTitle(), notification.getBody(), notification.getLinkPath(),
                notification.getType().name() + ":" + notification.getDeduplicationKey());
    }
}

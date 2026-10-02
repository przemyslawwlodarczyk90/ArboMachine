package com.example.arbomachine.domain.notification.delivery;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Wysylka powiadomien w tle (maly, ograniczony pulpit watkow - kolejka w pamieci, przepada przy restarcie; DLUG)
 * oraz harmonogram zadan okresowych (przypomnienia o pracach - WorkDueReminderJob).
 */
@Configuration
@EnableScheduling
public class NotificationDeliveryConfig {

    public static final String EXECUTOR_BEAN_NAME = "notificationDeliveryExecutor";

    @Bean(name = EXECUTOR_BEAN_NAME)
    public Executor notificationDeliveryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notification-delivery-");
        executor.initialize();
        return executor;
    }
}

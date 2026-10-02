package com.example.arbomachine.domain.notification.reminder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Codzienne przypomnienia o pracach na jutro - domyslnie 7:00 czasu polskiego (app.notifications.work-reminders.cron).
 * Wylaczenie: app.notifications.work-reminders.enabled=false. Jedna instancja aplikacji (V1) - przy wielu
 * instancjach chroni tylko deduplikacja (DLUG: blokada zadania, np. ShedLock).
 */
@Component
@ConditionalOnProperty(name = "app.notifications.work-reminders.enabled", havingValue = "true", matchIfMissing = true)
public class WorkDueReminderJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkDueReminderJob.class);
    private static final ZoneId POLAND = ZoneId.of("Europe/Warsaw");

    private final SendWorkDueRemindersHandler sendWorkDueRemindersHandler;

    public WorkDueReminderJob(SendWorkDueRemindersHandler sendWorkDueRemindersHandler) {
        this.sendWorkDueRemindersHandler = sendWorkDueRemindersHandler;
    }

    @Scheduled(cron = "${app.notifications.work-reminders.cron:0 0 7 * * *}", zone = "Europe/Warsaw")
    public void sendDailyReminders() {
        int createdCount = sendWorkDueRemindersHandler.handle(LocalDate.now(POLAND));
        LOGGER.info("Work due reminders: {} notification(s) created", createdCount);
    }
}

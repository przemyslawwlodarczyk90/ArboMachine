package com.example.arbomachine.domain.notification.reminder;

import com.example.arbomachine.domain.notification.entity.UserNotificationType;
import com.example.arbomachine.domain.notification.delivery.NotificationDispatcher;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.treework.entity.TreeWorkStatus;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Regula przypomnien V1 (etap 30): prace ZAPLANOWANE (PLANNED) z terminem na jutro -> JEDNO zbiorcze powiadomienie
 * na uzytkownika i dzien (deduplikacja "works-due:<data>" - ponowne uruchomienie tego samego dnia nic nie wysyla).
 * Zalecenia (RECOMMENDED) bez przypomnien - nie sa jeszcze przyjete do planu. Zwraca liczbe nowych powiadomien.
 */
@Component
public class SendWorkDueRemindersHandler {

    private static final int LISTED_WORKS_LIMIT = 3;
    private static final DateTimeFormatter POLISH_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final TreeWorkRepository treeWorkRepository;
    private final NotificationDispatcher notificationDispatcher;

    public SendWorkDueRemindersHandler(TreeWorkRepository treeWorkRepository, NotificationDispatcher notificationDispatcher) {
        this.treeWorkRepository = treeWorkRepository;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional(readOnly = true)
    public int handle(LocalDate today) {
        LocalDate tomorrow = today.plusDays(1);
        Map<Long, List<TreeWork>> worksByOwnerId = treeWorkRepository
                .findAllWithOwnerByStatusAndPlannedDate(TreeWorkStatus.PLANNED, tomorrow).stream()
                .collect(Collectors.groupingBy(work -> work.getTree().getProject().getOwner().getId(), LinkedHashMap::new,
                        Collectors.toList()));
        int createdCount = 0;
        for (Map.Entry<Long, List<TreeWork>> ownerWorks : worksByOwnerId.entrySet()) {
            List<TreeWork> works = ownerWorks.getValue();
            List<Long> projectIds = works.stream().map(work -> work.getTree().getProject().getId()).distinct().toList();
            String listed = works.stream().limit(LISTED_WORKS_LIMIT)
                    .map(work -> "drzewo " + work.getTree().getInventoryNumber() + " – " + work.getName()
                            + " (" + work.getTree().getProject().getName() + ")")
                    .collect(Collectors.joining("; "));
            String more = works.size() > LISTED_WORKS_LIMIT ? " i jeszcze " + (works.size() - LISTED_WORKS_LIMIT) : "";
            boolean created = notificationDispatcher.notifyUser(ownerWorks.getKey(), UserNotificationType.WORKS_DUE_TOMORROW,
                    "Jutro (" + tomorrow.format(POLISH_DATE) + ") zaplanowane prace: " + works.size(),
                    listed + more + ".",
                    projectIds.size() == 1 ? "/projects/" + projectIds.getFirst() + "/management" : "/projects",
                    "works-due:" + tomorrow).isPresent();
            if (created) createdCount++;
        }
        return createdCount;
    }
}

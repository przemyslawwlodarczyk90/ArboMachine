package com.example.arbomachine.domain.notification.delivery;

import com.example.arbomachine.domain.notification.entity.UserNotificationType;
import com.example.arbomachine.domain.treemonitoring.event.TreeMonitoringRecordSavedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

/**
 * Regula alertu V1 (etap 29): zapis kontroli z ocena "zly" (POOR) -> powiadomienie wlasciciela projektu. Edycja
 * wpisu, ktory juz mial ocene "zly", nie wysyla drugiego alertu. Po zatwierdzeniu transakcji - wycofany zapis
 * niczego nie wysyla. Progi i inne reguly - DO_DOPRECYZOWANIA (kto definiuje "stan zly").
 */
@Component
public class TreePoorHealthAlertListener {

    private static final String POOR = "POOR";
    private static final DateTimeFormatter POLISH_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final NotificationDispatcher notificationDispatcher;

    public TreePoorHealthAlertListener(NotificationDispatcher notificationDispatcher) {
        this.notificationDispatcher = notificationDispatcher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMonitoringRecordSaved(TreeMonitoringRecordSavedEvent event) {
        if (!POOR.equals(event.healthStatus()) || POOR.equals(event.previousHealthStatus())) return;
        notificationDispatcher.notifyUser(event.ownerUserAccountId(), UserNotificationType.TREE_POOR_HEALTH,
                "Drzewo " + event.treeInventoryNumber() + ": stan zły",
                "Kontrola z " + event.inspectionDate().format(POLISH_DATE) + " w projekcie „" + event.projectName()
                        + "” oznaczyła stan drzewa jako zły.",
                "/projects/" + event.projectId() + "/trees/" + event.treeId(),
                "tree-poor-health:" + event.recordId() + ":" + event.inspectionDate());
    }
}

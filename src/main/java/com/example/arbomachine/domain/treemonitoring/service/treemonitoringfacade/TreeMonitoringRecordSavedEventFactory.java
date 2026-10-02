package com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.treemonitoring.entity.TreeHealthStatus;
import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;
import com.example.arbomachine.domain.treemonitoring.event.TreeMonitoringRecordSavedEvent;

/** Sklada zdarzenie zapisu wpisu z encji (w transakcji - leniwe drzewo i projekt sa jeszcze dostepne). */
final class TreeMonitoringRecordSavedEventFactory {

    private TreeMonitoringRecordSavedEventFactory() {
    }

    static TreeMonitoringRecordSavedEvent eventOf(TreeMonitoringRecord savedRecord, TreeHealthStatus previousHealthStatus,
                                                  boolean newRecord) {
        Project project = savedRecord.getTree().getProject();
        return new TreeMonitoringRecordSavedEvent(savedRecord.getId(), project.getOwner().getId(), project.getId(),
                project.getName(), savedRecord.getTree().getId(), savedRecord.getTree().getInventoryNumber(),
                savedRecord.getInspectionDate(),
                savedRecord.getHealthStatus() == null ? null : savedRecord.getHealthStatus().name(),
                previousHealthStatus == null ? null : previousHealthStatus.name(), newRecord);
    }
}

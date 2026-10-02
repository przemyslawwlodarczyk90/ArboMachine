package com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade;

import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordRequest;
import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordResponse;
import com.example.arbomachine.domain.treemonitoring.entity.TreeHealthStatus;
import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Edycja wpisu - komplet pol (PUT; ostatni zapis wygrywa, jak w trybie offline). Bez zmian = bez zapisu i bez
 * zdarzenia. Wpis musi nalezec dokladnie do drzewa z URL (a drzewo do wlasnego projektu).
 */
@Component
public class UpdateTreeMonitoringRecordHandler {
    private final FindTreeMonitoringRecordOfTreeInProjectOwnedByCurrentUserHandler findRecordHandler;
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UpdateTreeMonitoringRecordHandler(FindTreeMonitoringRecordOfTreeInProjectOwnedByCurrentUserHandler findRecordHandler,
                                             TreeMonitoringRecordRepository treeMonitoringRecordRepository,
                                             ApplicationEventPublisher applicationEventPublisher) {
        this.findRecordHandler = findRecordHandler;
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public TreeMonitoringRecordResponse handle(String currentUserEmail, Long projectId, Long treeId, Long recordId,
                                               TreeMonitoringRecordRequest request) {
        TreeMonitoringRecord record = findRecordHandler.handle(currentUserEmail, projectId, treeId, recordId);
        TreeMonitoringRecord.Fields fields = TreeMonitoringRecordMapper.fieldsOf(request);
        if (!record.differsFrom(fields)) {
            return TreeMonitoringRecordMapper.toResponse(record);
        }
        TreeHealthStatus previousHealthStatus = record.getHealthStatus();
        record.replaceFields(fields, Instant.now().truncatedTo(ChronoUnit.MICROS));
        TreeMonitoringRecord savedRecord = treeMonitoringRecordRepository.save(record);
        applicationEventPublisher.publishEvent(TreeMonitoringRecordSavedEventFactory.eventOf(savedRecord, previousHealthStatus, false));
        return TreeMonitoringRecordMapper.toResponse(savedRecord);
    }
}

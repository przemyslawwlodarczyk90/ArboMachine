package com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordRequest;
import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordResponse;
import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Nowy wpis monitoringu drzewa w WLASNYM projekcie; po zapisie publikuje TreeMonitoringRecordSavedEvent. */
@Component
public class CreateTreeMonitoringRecordHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public CreateTreeMonitoringRecordHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                             TreeMonitoringRecordRepository treeMonitoringRecordRepository,
                                             ApplicationEventPublisher applicationEventPublisher) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public TreeMonitoringRecordResponse handle(String currentUserEmail, Long projectId, Long treeId,
                                               TreeMonitoringRecordRequest request) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL (jak przy drzewach).
        TreeMonitoringRecord savedRecord = treeMonitoringRecordRepository.save(TreeMonitoringRecord.createForTree(
                treeInOwnedProject, TreeMonitoringRecordMapper.fieldsOf(request), Instant.now().truncatedTo(ChronoUnit.MICROS)));
        applicationEventPublisher.publishEvent(TreeMonitoringRecordSavedEventFactory.eventOf(savedRecord, null, true));
        return TreeMonitoringRecordMapper.toResponse(savedRecord);
    }
}

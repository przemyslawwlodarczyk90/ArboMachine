package com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade;

import com.example.arbomachine.domain.tree.entity.ClientRequestId;
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
import java.util.Optional;

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

    /**
     * Etap 36: clientRequestId (UUID z urzadzenia, null = brak) - ponowienie zwraca juz zapisana kontrole (bez drugiego
     * powiadomienia o zlym stanie).
     */
    @Transactional
    public TreeMonitoringRecordResponse handle(String currentUserEmail, Long projectId, Long treeId,
                                               TreeMonitoringRecordRequest request, String clientRequestId) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        String normalizedClientRequestId = ClientRequestId.normalizedOrNull(clientRequestId);
        if (normalizedClientRequestId != null) {
            Optional<TreeMonitoringRecord> alreadyCreatedRecord = treeMonitoringRecordRepository
                    .findByTreeIdAndClientRequestId(treeInOwnedProject.getId(), normalizedClientRequestId);
            if (alreadyCreatedRecord.isPresent()) return TreeMonitoringRecordMapper.toResponse(alreadyCreatedRecord.get());
        }
        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL (jak przy drzewach).
        TreeMonitoringRecord newRecord = TreeMonitoringRecord.createForTree(
                treeInOwnedProject, TreeMonitoringRecordMapper.fieldsOf(request), Instant.now().truncatedTo(ChronoUnit.MICROS));
        newRecord.assignClientRequestId(normalizedClientRequestId);
        TreeMonitoringRecord savedRecord = treeMonitoringRecordRepository.save(newRecord);
        applicationEventPublisher.publishEvent(TreeMonitoringRecordSavedEventFactory.eventOf(savedRecord, null, true));
        return TreeMonitoringRecordMapper.toResponse(savedRecord);
    }
}

package com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade;

import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Usuniecie wpisu - tylko z drzewa z URL w WLASNYM projekcie; inaczej 404 i nic nie jest usuwane. */
@Component
public class DeleteTreeMonitoringRecordHandler {
    private final FindTreeMonitoringRecordOfTreeInProjectOwnedByCurrentUserHandler findRecordHandler;
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;

    public DeleteTreeMonitoringRecordHandler(FindTreeMonitoringRecordOfTreeInProjectOwnedByCurrentUserHandler findRecordHandler,
                                             TreeMonitoringRecordRepository treeMonitoringRecordRepository) {
        this.findRecordHandler = findRecordHandler;
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
    }

    @Transactional
    public void handle(String currentUserEmail, Long projectId, Long treeId, Long recordId) {
        treeMonitoringRecordRepository.delete(findRecordHandler.handle(currentUserEmail, projectId, treeId, recordId));
    }
}

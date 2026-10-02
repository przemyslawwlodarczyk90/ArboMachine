package com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;
import com.example.arbomachine.domain.treemonitoring.exception.TreeMonitoringRecordNotFoundException;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import org.springframework.stereotype.Component;

/**
 * Wspolny handler: wpis o podanym id, ktory nalezy DOKLADNIE do drzewa z URL, drzewo - do projektu z URL, a projekt -
 * do zalogowanego uzytkownika. Kazde naruszenie hierarchii = 404 (jak przy pracach).
 */
@Component
public class FindTreeMonitoringRecordOfTreeInProjectOwnedByCurrentUserHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;

    public FindTreeMonitoringRecordOfTreeInProjectOwnedByCurrentUserHandler(
            FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
            TreeMonitoringRecordRepository treeMonitoringRecordRepository) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
    }

    public TreeMonitoringRecord handle(String currentUserEmail, Long projectId, Long treeId, Long recordId) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        return treeMonitoringRecordRepository.findByIdAndTreeId(recordId, treeInOwnedProject.getId())
                .orElseThrow(() -> new TreeMonitoringRecordNotFoundException("Monitoring record not found"));
    }
}

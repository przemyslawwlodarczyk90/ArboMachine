package com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordResponse;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Historia kontroli drzewa z WLASNEGO projektu - najnowsza pierwsza. Pusta lista = drzewo nie sprawdzone. */
@Component
public class GetTreeMonitoringRecordsHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;

    public GetTreeMonitoringRecordsHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                           TreeMonitoringRecordRepository treeMonitoringRecordRepository) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<TreeMonitoringRecordResponse> handle(String currentUserEmail, Long projectId, Long treeId) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        return treeMonitoringRecordRepository.findAllByTreeIdOrderByInspectionDateDescIdDesc(treeInOwnedProject.getId())
                .stream().map(TreeMonitoringRecordMapper::toResponse).toList();
    }
}

package com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordResponse;
import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ostatnia kontrola kazdego drzewa WLASNEGO projektu (lista drzew: "Ostatnia kontrola" / "Nie sprawdzone") jednym
 * zapytaniem. Drzewa bez wpisow nie ma na liscie. Cudzy projekt -> 404.
 */
@Component
public class GetLatestTreeMonitoringRecordsOfProjectHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;

    public GetLatestTreeMonitoringRecordsOfProjectHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                                          TreeMonitoringRecordRepository treeMonitoringRecordRepository) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<TreeMonitoringRecordResponse> handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        Map<Long, TreeMonitoringRecord> latestByTreeId = new LinkedHashMap<>();
        // Zapytanie zwraca wpisy kazdego drzewa od najnowszego - pierwszy wpis drzewa to ostatnia kontrola.
        for (TreeMonitoringRecord record : treeMonitoringRecordRepository.findAllOfProjectNewestFirst(ownedProject.getId())) {
            latestByTreeId.putIfAbsent(record.getTree().getId(), record);
        }
        return latestByTreeId.values().stream().map(TreeMonitoringRecordMapper::toResponse).toList();
    }
}

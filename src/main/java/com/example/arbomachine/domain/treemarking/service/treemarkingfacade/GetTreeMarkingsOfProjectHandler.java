package com.example.arbomachine.domain.treemarking.service.treemarkingfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingCatalog;
import com.example.arbomachine.domain.treemarking.dto.TreeMarkingsResponse;
import com.example.arbomachine.infrastructure.repository.TreeMarkingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Oznaczenia wszystkich drzew wlasnego projektu (tylko drzewa, ktore je maja) - mapa i lista drzew jednym zapytaniem. */
@Component
public class GetTreeMarkingsOfProjectHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeMarkingRepository treeMarkingRepository;
    private final TreeMarkingCatalog treeMarkingCatalog;

    public GetTreeMarkingsOfProjectHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                           TreeMarkingRepository treeMarkingRepository, TreeMarkingCatalog treeMarkingCatalog) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeMarkingRepository = treeMarkingRepository;
        this.treeMarkingCatalog = treeMarkingCatalog;
    }

    @Transactional(readOnly = true)
    public List<TreeMarkingsResponse> handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        Map<Long, List<String>> codesByTreeId = new LinkedHashMap<>();
        for (TreeMarkingRepository.TreeIdAndCode row : treeMarkingRepository.findTreeIdsAndCodesByProjectId(ownedProject.getId())) {
            codesByTreeId.computeIfAbsent(row.getTreeId(), treeId -> new ArrayList<>()).add(row.getCode());
        }
        return codesByTreeId.entrySet().stream()
                .map(entry -> new TreeMarkingsResponse(entry.getKey(), treeMarkingCatalog.inCatalogOrder(entry.getValue())))
                .filter(response -> !response.codes().isEmpty())
                .toList();
    }
}

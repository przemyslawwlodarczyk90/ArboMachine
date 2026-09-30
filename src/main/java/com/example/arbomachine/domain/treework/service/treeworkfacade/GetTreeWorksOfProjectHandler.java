package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.mapper.TreeWorkMapper;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Prace wszystkich drzew WLASNEGO projektu - dla mapy projektu (symbole prac, legenda) jednym zapytaniem zamiast
 * osobnego zapytania o kazde drzewo. Wlasnosc sprawdzana raz, na projekcie; cudzy projekt -> 404.
 */
@Component
public class GetTreeWorksOfProjectHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeWorkRepository treeWorkRepository;
    private final TreeWorkMapper treeWorkMapper;

    public GetTreeWorksOfProjectHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                        TreeWorkRepository treeWorkRepository, TreeWorkMapper treeWorkMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeWorkRepository = treeWorkRepository;
        this.treeWorkMapper = treeWorkMapper;
    }

    public List<TreeWorkResponse> handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeWorkRepository.findAllByTreeProjectIdOrderByTreeIdAscCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .map(treeWorkMapper::toTreeWorkResponse)
                .toList();
    }
}

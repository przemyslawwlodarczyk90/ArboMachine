package com.example.arbomachine.domain.treestand.service.treestandfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treestand.dto.TreeStandResponse;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Grupy drzew wlasnego projektu (wg nazwy) z przypisanymi drzewami - lista, mapa, filtr listy drzew. */
@Component
public class GetTreeStandsOfProjectHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeStandRepository treeStandRepository;
    private final TreeStandMapper treeStandMapper;

    public GetTreeStandsOfProjectHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                         TreeStandRepository treeStandRepository, TreeStandMapper treeStandMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeStandRepository = treeStandRepository;
        this.treeStandMapper = treeStandMapper;
    }

    @Transactional(readOnly = true)
    public List<TreeStandResponse> handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeStandRepository.findAllByProjectIdOrderByNameAscIdAsc(ownedProject.getId()).stream()
                .map(treeStandMapper::toResponse).toList();
    }
}

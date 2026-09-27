package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.exception.TreeNotFoundException;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import org.springframework.stereotype.Component;

/**
 * Wspolny handler: drzewo o podanym id, ktore nalezy DOKLADNIE do projektu z URL, a ten projekt - do zalogowanego
 * uzytkownika. Najpierw wlasnosc projektu (cudzy/nieistniejacy -> ProjectNotFoundException), potem drzewo zawezone
 * do tego projektu (drzewo z innego projektu albo nieistniejace -> TreeNotFoundException). Oba -> 404.
 */
@Component
public class FindTreeInProjectOwnedByCurrentUserHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeRepository treeRepository;

    public FindTreeInProjectOwnedByCurrentUserHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                                      TreeRepository treeRepository) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeRepository = treeRepository;
    }

    public Tree handle(String currentUserEmail, Long projectId, Long treeId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeRepository.findByIdAndProjectId(treeId, ownedProject.getId())
                .orElseThrow(() -> new TreeNotFoundException("Tree not found"));
    }
}

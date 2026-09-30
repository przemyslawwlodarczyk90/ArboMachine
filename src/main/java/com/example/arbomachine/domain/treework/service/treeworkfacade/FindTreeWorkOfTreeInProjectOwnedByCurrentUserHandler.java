package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.treework.exception.TreeWorkNotFoundException;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import org.springframework.stereotype.Component;

/**
 * Wspolny handler: praca o podanym id, ktora nalezy DOKLADNIE do drzewa z URL, drzewo - do projektu z URL,
 * a projekt - do zalogowanego uzytkownika. Kolejnosc sprawdzen: projekt (404), drzewo w projekcie (404),
 * praca w drzewie (404). Praca z innego drzewa - nawet tego samego uzytkownika - daje 404, a nie cudze dane.
 */
@Component
public class FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeWorkRepository treeWorkRepository;

    public FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler(
            FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
            TreeWorkRepository treeWorkRepository) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeWorkRepository = treeWorkRepository;
    }

    public TreeWork handle(String currentUserEmail, Long projectId, Long treeId, Long treeWorkId) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        return treeWorkRepository.findByIdAndTreeId(treeWorkId, treeInOwnedProject.getId())
                .orElseThrow(() -> new TreeWorkNotFoundException("Tree work not found"));
    }
}

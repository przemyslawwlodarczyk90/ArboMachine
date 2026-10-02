package com.example.arbomachine.domain.treestand.service.treestandfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treestand.entity.TreeStand;
import com.example.arbomachine.domain.treestand.exception.TreeStandNotFoundException;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import org.springframework.stereotype.Component;

/** Grupa o podanym id DOKLADNIE z projektu z URL, a projekt - zalogowanego uzytkownika; inaczej 404. */
@Component
public class FindTreeStandOfProjectOwnedByCurrentUserHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeStandRepository treeStandRepository;

    public FindTreeStandOfProjectOwnedByCurrentUserHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                                           TreeStandRepository treeStandRepository) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeStandRepository = treeStandRepository;
    }

    public TreeStand handle(String currentUserEmail, Long projectId, Long standId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeStandRepository.findByIdAndProjectId(standId, ownedProject.getId())
                .orElseThrow(() -> new TreeStandNotFoundException("Tree stand not found"));
    }
}

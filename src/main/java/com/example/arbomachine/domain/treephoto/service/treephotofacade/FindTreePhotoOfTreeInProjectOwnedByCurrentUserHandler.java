package com.example.arbomachine.domain.treephoto.service.treephotofacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoNotFoundException;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import org.springframework.stereotype.Component;

/**
 * Wspolny handler: zdjecie o podanym id, ktore nalezy DOKLADNIE do drzewa z URL, drzewo - do projektu z URL,
 * a projekt - do zalogowanego uzytkownika. Nigdy samo findById(photoId) - to umozliwialoby IDOR.
 * Zdjecie z innego drzewa (nawet tego samego uzytkownika) daje 404.
 */
@Component
public class FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreePhotoRepository treePhotoRepository;

    public FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler(
            FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
            TreePhotoRepository treePhotoRepository) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treePhotoRepository = treePhotoRepository;
    }

    public TreePhoto handle(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        return treePhotoRepository.findByIdAndTreeId(treePhotoId, treeInOwnedProject.getId())
                .orElseThrow(() -> new TreePhotoNotFoundException("Tree photo not found"));
    }
}

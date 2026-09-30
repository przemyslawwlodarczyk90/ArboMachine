package com.example.arbomachine.domain.treephoto.service.treephotofacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoResponse;
import com.example.arbomachine.domain.treephoto.mapper.TreePhotoMapper;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Metadane zdjec drzewa z WLASNEGO projektu, w kolejnosci dodawania. */
@Component
public class GetTreePhotosHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreePhotoRepository treePhotoRepository;
    private final TreePhotoMapper treePhotoMapper;

    public GetTreePhotosHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                TreePhotoRepository treePhotoRepository, TreePhotoMapper treePhotoMapper) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treePhotoRepository = treePhotoRepository;
        this.treePhotoMapper = treePhotoMapper;
    }

    public List<TreePhotoResponse> handle(String currentUserEmail, Long projectId, Long treeId) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        return treePhotoRepository.findAllByTreeIdOrderByCreatedAtAscIdAsc(treeInOwnedProject.getId()).stream()
                .map(treePhotoMapper::toTreePhotoResponse)
                .toList();
    }
}

package com.example.arbomachine.domain.treephoto.service.treephotofacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoResponse;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.treephoto.mapper.TreePhotoMapper;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Metadane zdjec drzewa z WLASNEGO projektu, w kolejnosci dodawania; informacja o adnotacjach jednym zapytaniem. */
@Component
public class GetTreePhotosHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreePhotoRepository treePhotoRepository;
    private final TreePhotoMapper treePhotoMapper;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;

    public GetTreePhotosHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                TreePhotoRepository treePhotoRepository, TreePhotoMapper treePhotoMapper,
                                TreePhotoAnnotationRepository treePhotoAnnotationRepository) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treePhotoRepository = treePhotoRepository;
        this.treePhotoMapper = treePhotoMapper;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
    }

    public List<TreePhotoResponse> handle(String currentUserEmail, Long projectId, Long treeId) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        List<TreePhoto> treePhotos = treePhotoRepository.findAllByTreeIdOrderByCreatedAtAscIdAsc(treeInOwnedProject.getId());
        Map<Long, TreePhotoAnnotation> annotationByPhotoId = treePhotoAnnotationRepository
                .findAllByTreePhotoIdIn(treePhotos.stream().map(TreePhoto::getId).toList()).stream()
                .collect(Collectors.toMap(annotation -> annotation.getTreePhoto().getId(), Function.identity()));
        return treePhotos.stream()
                .map(treePhoto -> treePhotoMapper.toTreePhotoResponse(treePhoto, annotationByPhotoId.get(treePhoto.getId())))
                .toList();
    }
}

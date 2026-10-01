package com.example.arbomachine.domain.treephoto.service.treephotofacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treephoto.dto.ProjectPhotoResponse;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Zdjecia wszystkich drzew WLASNEGO projektu (materialy do dokumentacji) - wlasnosc sprawdzana raz, na projekcie;
 * zdjecia z drzewami jednym zapytaniem, adnotacje drugim (bez N+1).
 */
@Component
public class GetProjectPhotosHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreePhotoRepository treePhotoRepository;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;

    public GetProjectPhotosHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                   TreePhotoRepository treePhotoRepository,
                                   TreePhotoAnnotationRepository treePhotoAnnotationRepository) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treePhotoRepository = treePhotoRepository;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
    }

    public List<ProjectPhotoResponse> handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        List<TreePhoto> treePhotos = treePhotoRepository.findAllOfProjectWithTree(ownedProject.getId());
        Map<Long, TreePhotoAnnotation> annotationByPhotoId = treePhotoAnnotationRepository
                .findAllByTreePhotoIdIn(treePhotos.stream().map(TreePhoto::getId).toList()).stream()
                .collect(Collectors.toMap(annotation -> annotation.getTreePhoto().getId(), Function.identity()));
        return treePhotos.stream().map(treePhoto -> {
            TreePhotoAnnotation annotation = annotationByPhotoId.get(treePhoto.getId());
            return new ProjectPhotoResponse(treePhoto.getId(), treePhoto.getTree().getId(),
                    treePhoto.getTree().getInventoryNumber(), treePhoto.getOriginalFileName(), treePhoto.getContentType(),
                    treePhoto.getFileSizeBytes(), annotation != null, annotation == null ? null : annotation.getUpdatedAt(),
                    treePhoto.getCreatedAt());
        }).toList();
    }
}

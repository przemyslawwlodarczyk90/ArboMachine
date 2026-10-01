package com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade;

import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationResponse;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoAnnotationNotFoundException;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Aktualna adnotacja zdjecia (cala hierarchia wlasnosci); brak adnotacji -> 404 jak kazdy nieistniejacy zasob. */
@Component
public class GetTreePhotoAnnotationHandler {
    private final FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    private final ObjectMapper objectMapper;

    public GetTreePhotoAnnotationHandler(FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler,
                                         TreePhotoAnnotationRepository treePhotoAnnotationRepository,
                                         ObjectMapper objectMapper) {
        this.findTreePhotoHandler = findTreePhotoHandler;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.objectMapper = objectMapper;
    }

    public PhotoAnnotationResponse handle(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        TreePhoto treePhoto = findTreePhotoHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
        return treePhotoAnnotationRepository.findByTreePhotoId(treePhoto.getId())
                .map(annotation -> TreePhotoAnnotationMapper.toResponse(objectMapper, annotation))
                .orElseThrow(() -> new TreePhotoAnnotationNotFoundException("Tree photo annotation not found"));
    }
}

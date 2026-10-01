package com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade;

import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoAnnotationNotFoundException;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Usuniecie adnotacji: rekord, potem splaszczony PNG (jak przy zdjeciach - osierocony plik lepszy niz rekord bez pliku).
 * Oryginalne zdjecie i jego plik zostaja nietkniete. Brak adnotacji -> 404.
 */
@Component
public class DeleteTreePhotoAnnotationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteTreePhotoAnnotationHandler.class);

    private final FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    private final PhotoStorage photoStorage;

    public DeleteTreePhotoAnnotationHandler(FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler,
                                            TreePhotoAnnotationRepository treePhotoAnnotationRepository,
                                            PhotoStorage photoStorage) {
        this.findTreePhotoHandler = findTreePhotoHandler;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.photoStorage = photoStorage;
    }

    public void handle(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        TreePhoto treePhoto = findTreePhotoHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
        TreePhotoAnnotation annotation = treePhotoAnnotationRepository.findByTreePhotoId(treePhoto.getId())
                .orElseThrow(() -> new TreePhotoAnnotationNotFoundException("Tree photo annotation not found"));
        treePhotoAnnotationRepository.delete(annotation);
        try {
            photoStorage.delete(annotation.getRenderedStorageKey());
        } catch (RuntimeException fileDeletionError) {
            LOGGER.warn("Annotation of photo {} deleted, but rendered file {} could not be deleted (orphaned file)",
                    treePhoto.getId(), annotation.getRenderedStorageKey(), fileDeletionError);
        }
    }
}

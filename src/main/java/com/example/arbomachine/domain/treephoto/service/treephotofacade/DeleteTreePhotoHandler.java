package com.example.arbomachine.domain.treephoto.service.treephotofacade;

import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Usuniecie zdjecia - tylko z drzewa z URL w WLASNYM projekcie. Najpierw rekordy w bazie w jednej transakcji
 * (adnotacja zdjecia, jesli jest - wymaga tego klucz obcy - i samo zdjecie), potem pliki: oryginal i splaszczony PNG
 * adnotacji. Jesli pliku nie da sie usunac, zostaje osierocony plik (logujemy go) - to mniejsze zlo niz rekord
 * wskazujacy na nieistniejacy plik. Pliki usuwamy dopiero po faktycznym commicie rekordow.
 */
@Component
public class DeleteTreePhotoHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteTreePhotoHandler.class);

    private final FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
    private final TreePhotoRepository treePhotoRepository;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    private final PhotoStorage photoStorage;
    private final TransactionTemplate transactionTemplate;

    public DeleteTreePhotoHandler(
            FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler,
            TreePhotoRepository treePhotoRepository, TreePhotoAnnotationRepository treePhotoAnnotationRepository,
            PhotoStorage photoStorage, TransactionTemplate transactionTemplate) {
        this.findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler = findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
        this.treePhotoRepository = treePhotoRepository;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.photoStorage = photoStorage;
        this.transactionTemplate = transactionTemplate;
    }

    public void handle(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        TreePhoto treePhoto = findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId,
                treeId, treePhotoId);
        Optional<TreePhotoAnnotation> annotation = treePhotoAnnotationRepository.findByTreePhotoId(treePhoto.getId());
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            annotation.ifPresent(treePhotoAnnotationRepository::delete);
            treePhotoRepository.delete(treePhoto);
        });
        List<String> storageKeysToDelete = new ArrayList<>(List.of(treePhoto.getStorageKey()));
        annotation.ifPresent(existingAnnotation -> storageKeysToDelete.add(existingAnnotation.getRenderedStorageKey()));
        for (String storageKey : storageKeysToDelete) {
            try {
                photoStorage.delete(storageKey);
            } catch (RuntimeException fileDeletionError) {
                LOGGER.warn("Photo {} deleted from database, but file {} could not be deleted (orphaned file)",
                        treePhoto.getId(), storageKey, fileDeletionError);
            }
        }
    }
}

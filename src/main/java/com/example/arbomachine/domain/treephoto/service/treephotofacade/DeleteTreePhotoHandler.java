package com.example.arbomachine.domain.treephoto.service.treephotofacade;

import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Usuniecie zdjecia - tylko z drzewa z URL w WLASNYM projekcie. Kolejnosc: najpierw rekord w bazie (commit
 * w repozytorium), potem plik. Jesli plik nie da sie usunac, zostaje osierocony plik (logujemy go) - to mniejsze
 * zlo niz rekord wskazujacy na nieistniejacy plik (zepsute zdjecie w UI). Handler celowo nie jest @Transactional,
 * zeby plik byl usuwany dopiero po faktycznym usunieciu rekordu.
 */
@Component
public class DeleteTreePhotoHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteTreePhotoHandler.class);

    private final FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
    private final TreePhotoRepository treePhotoRepository;
    private final PhotoStorage photoStorage;

    public DeleteTreePhotoHandler(
            FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler,
            TreePhotoRepository treePhotoRepository, PhotoStorage photoStorage) {
        this.findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler = findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
        this.treePhotoRepository = treePhotoRepository;
        this.photoStorage = photoStorage;
    }

    public void handle(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        TreePhoto treePhoto = findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId,
                treeId, treePhotoId);
        treePhotoRepository.delete(treePhoto);
        try {
            photoStorage.delete(treePhoto.getStorageKey());
        } catch (RuntimeException fileDeletionError) {
            LOGGER.warn("Photo {} deleted from database, but its file {} could not be deleted (orphaned file)",
                    treePhoto.getId(), treePhoto.getStorageKey(), fileDeletionError);
        }
    }
}

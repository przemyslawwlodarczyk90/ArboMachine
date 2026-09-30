package com.example.arbomachine.domain.treephoto.service.treephotofacade;

import com.example.arbomachine.domain.treephoto.dto.TreePhotoContent;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoNotFoundException;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tresc zdjecia - tylko po przejsciu calej hierarchii (wlasny projekt -> drzewo -> zdjecie tego drzewa).
 * Rekord bez pliku w storage (np. plik usuniety recznie z dysku) konczy sie 404, a nie bledem serwera.
 */
@Component
public class GetTreePhotoContentHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetTreePhotoContentHandler.class);

    private final FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
    private final PhotoStorage photoStorage;

    public GetTreePhotoContentHandler(
            FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler,
            PhotoStorage photoStorage) {
        this.findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler = findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
        this.photoStorage = photoStorage;
    }

    public TreePhotoContent handle(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        TreePhoto treePhoto = findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId,
                treeId, treePhotoId);
        try {
            return new TreePhotoContent(photoStorage.load(treePhoto.getStorageKey()), treePhoto.getContentType(),
                    treePhoto.getOriginalFileName(), treePhoto.getFileSizeBytes());
        } catch (PhotoStorageException missingFileError) {
            LOGGER.error("Photo {} has metadata but its file is missing in storage", treePhoto.getId(), missingFileError);
            throw new TreePhotoNotFoundException("Tree photo not found");
        }
    }
}

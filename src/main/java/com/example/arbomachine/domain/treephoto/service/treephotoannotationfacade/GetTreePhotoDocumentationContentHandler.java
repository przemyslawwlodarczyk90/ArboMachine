package com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade;

import com.example.arbomachine.domain.treephoto.dto.TreePhotoContent;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoNotFoundException;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorageException;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stabilny kontrakt dla dokumentacji: wersja adnotowana (PNG), jesli istnieje, w przeciwnym razie oryginal.
 * Przyszly generator dokumentow nie musi wiedziec, czy zdjecie ma adnotacje. Gdy plik adnotacji zginal
 * z dysku, wracamy do oryginalu (lepsze zdjecie bez szkicu niz brak zdjecia w dokumentacji).
 */
@Component
public class GetTreePhotoDocumentationContentHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetTreePhotoDocumentationContentHandler.class);

    private final FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    private final PhotoStorage photoStorage;

    public GetTreePhotoDocumentationContentHandler(FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler,
                                                   TreePhotoAnnotationRepository treePhotoAnnotationRepository,
                                                   PhotoStorage photoStorage) {
        this.findTreePhotoHandler = findTreePhotoHandler;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.photoStorage = photoStorage;
    }

    public TreePhotoContent handle(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        TreePhoto treePhoto = findTreePhotoHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
        var annotation = treePhotoAnnotationRepository.findByTreePhotoId(treePhoto.getId());
        if (annotation.isPresent()) {
            try {
                return new TreePhotoContent(photoStorage.load(annotation.get().getRenderedStorageKey()),
                        annotation.get().getRenderedContentType(),
                        GetTreePhotoAnnotationContentHandler.annotatedFileNameOf(treePhoto),
                        annotation.get().getRenderedFileSizeBytes());
            } catch (PhotoStorageException missingFileError) {
                LOGGER.error("Rendered annotation of photo {} is missing - serving original", treePhoto.getId(), missingFileError);
            }
        }
        try {
            return new TreePhotoContent(photoStorage.load(treePhoto.getStorageKey()), treePhoto.getContentType(),
                    treePhoto.getOriginalFileName(), treePhoto.getFileSizeBytes());
        } catch (PhotoStorageException missingFileError) {
            throw new TreePhotoNotFoundException("Tree photo not found");
        }
    }
}

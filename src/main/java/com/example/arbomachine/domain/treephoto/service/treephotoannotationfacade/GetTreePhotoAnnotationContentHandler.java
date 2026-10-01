package com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade;

import com.example.arbomachine.domain.treephoto.dto.TreePhotoContent;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoAnnotationNotFoundException;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorageException;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import org.springframework.stereotype.Component;

/** Splaszczony PNG adnotacji (zdjecie + rysunek). Brak adnotacji albo brak pliku -> 404. */
@Component
public class GetTreePhotoAnnotationContentHandler {
    private final FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    private final PhotoStorage photoStorage;

    public GetTreePhotoAnnotationContentHandler(FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler,
                                                TreePhotoAnnotationRepository treePhotoAnnotationRepository,
                                                PhotoStorage photoStorage) {
        this.findTreePhotoHandler = findTreePhotoHandler;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.photoStorage = photoStorage;
    }

    public TreePhotoContent handle(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        TreePhoto treePhoto = findTreePhotoHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
        TreePhotoAnnotation annotation = treePhotoAnnotationRepository.findByTreePhotoId(treePhoto.getId())
                .orElseThrow(() -> new TreePhotoAnnotationNotFoundException("Tree photo annotation not found"));
        try {
            return new TreePhotoContent(photoStorage.load(annotation.getRenderedStorageKey()),
                    annotation.getRenderedContentType(), annotatedFileNameOf(treePhoto), annotation.getRenderedFileSizeBytes());
        } catch (PhotoStorageException missingFileError) {
            throw new TreePhotoAnnotationNotFoundException("Tree photo annotation not found");
        }
    }

    /** "IMG_0001.jpg" -> "IMG_0001-adnotacja.png" - nazwa pobieranego pliku (tylko do wyswietlenia). */
    static String annotatedFileNameOf(TreePhoto treePhoto) {
        String originalFileName = treePhoto.getOriginalFileName();
        int extensionStart = originalFileName.lastIndexOf('.');
        String baseName = extensionStart > 0 ? originalFileName.substring(0, extensionStart) : originalFileName;
        return baseName + "-adnotacja.png";
    }
}

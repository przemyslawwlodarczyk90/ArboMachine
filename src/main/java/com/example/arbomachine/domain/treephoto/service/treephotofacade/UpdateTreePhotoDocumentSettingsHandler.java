package com.example.arbomachine.domain.treephoto.service.treephotofacade;

import com.example.arbomachine.domain.treephoto.dto.TreePhotoDocumentSettingsRequest;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoResponse;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.mapper.TreePhotoMapper;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Etap 52: podpis zdjecia i wybor "w dokumencie" - tylko zdjecie drzewa z URL w WLASNYM projekcie. Podpis przyciety,
 * pusty = null (dokument uzyje nazwy pliku).
 */
@Component
public class UpdateTreePhotoDocumentSettingsHandler {
    private final FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
    private final TreePhotoRepository treePhotoRepository;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    private final TreePhotoMapper treePhotoMapper;

    public UpdateTreePhotoDocumentSettingsHandler(
            FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler,
            TreePhotoRepository treePhotoRepository, TreePhotoAnnotationRepository treePhotoAnnotationRepository,
            TreePhotoMapper treePhotoMapper) {
        this.findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler = findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
        this.treePhotoRepository = treePhotoRepository;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.treePhotoMapper = treePhotoMapper;
    }

    @Transactional
    public TreePhotoResponse handle(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId,
                                    TreePhotoDocumentSettingsRequest request) {
        TreePhoto treePhoto = findTreePhotoOfTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId,
                treeId, treePhotoId);
        String caption = request.caption();
        treePhoto.changeDocumentSettings(caption == null || caption.isBlank() ? null : caption.trim(),
                request.includedInDocument());
        TreePhoto savedTreePhoto = treePhotoRepository.save(treePhoto);
        return treePhotoMapper.toTreePhotoResponse(savedTreePhoto,
                treePhotoAnnotationRepository.findByTreePhotoId(savedTreePhoto.getId()).orElse(null));
    }
}

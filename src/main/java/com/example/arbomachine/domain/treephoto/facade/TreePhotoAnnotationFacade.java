package com.example.arbomachine.domain.treephoto.facade;

import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationDocumentRequest;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationResponse;
import com.example.arbomachine.domain.treephoto.dto.RenderedAnnotationImageUpload;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoContent;
import com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade.DeleteTreePhotoAnnotationHandler;
import com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade.GetTreePhotoAnnotationContentHandler;
import com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade.GetTreePhotoAnnotationHandler;
import com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade.GetTreePhotoDocumentationContentHandler;
import com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade.SaveTreePhotoAnnotationHandler;
import org.springframework.stereotype.Component;

/**
 * Adnotacje zdjec drzew (szkice na zdjeciach) i tresc zdjec do dokumentacji. Wlasnosc:
 * adnotacja -> zdjecie -> drzewo -> projekt -> owner. Tylko deleguje do handlerow z service/treephotoannotationfacade.
 */
@Component
public class TreePhotoAnnotationFacade {
    private final GetTreePhotoAnnotationHandler getTreePhotoAnnotationHandler;
    private final SaveTreePhotoAnnotationHandler saveTreePhotoAnnotationHandler;
    private final DeleteTreePhotoAnnotationHandler deleteTreePhotoAnnotationHandler;
    private final GetTreePhotoAnnotationContentHandler getTreePhotoAnnotationContentHandler;
    private final GetTreePhotoDocumentationContentHandler getTreePhotoDocumentationContentHandler;

    public TreePhotoAnnotationFacade(GetTreePhotoAnnotationHandler getTreePhotoAnnotationHandler,
                                     SaveTreePhotoAnnotationHandler saveTreePhotoAnnotationHandler,
                                     DeleteTreePhotoAnnotationHandler deleteTreePhotoAnnotationHandler,
                                     GetTreePhotoAnnotationContentHandler getTreePhotoAnnotationContentHandler,
                                     GetTreePhotoDocumentationContentHandler getTreePhotoDocumentationContentHandler) {
        this.getTreePhotoAnnotationHandler = getTreePhotoAnnotationHandler;
        this.saveTreePhotoAnnotationHandler = saveTreePhotoAnnotationHandler;
        this.deleteTreePhotoAnnotationHandler = deleteTreePhotoAnnotationHandler;
        this.getTreePhotoAnnotationContentHandler = getTreePhotoAnnotationContentHandler;
        this.getTreePhotoDocumentationContentHandler = getTreePhotoDocumentationContentHandler;
    }

    public PhotoAnnotationResponse getAnnotationOfPhotoOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                     Long treePhotoId) {
        return getTreePhotoAnnotationHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
    }

    public PhotoAnnotationResponse saveAnnotationOfPhotoOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                      Long treePhotoId, PhotoAnnotationDocumentRequest document,
                                                                      RenderedAnnotationImageUpload renderedImage) {
        return saveTreePhotoAnnotationHandler.handle(currentUserEmail, projectId, treeId, treePhotoId, document, renderedImage);
    }

    public void deleteAnnotationOfPhotoOfCurrentUser(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        deleteTreePhotoAnnotationHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
    }

    public TreePhotoContent getAnnotationContentOfPhotoOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                     Long treePhotoId) {
        return getTreePhotoAnnotationContentHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
    }

    public TreePhotoContent getDocumentationContentOfPhotoOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                        Long treePhotoId) {
        return getTreePhotoDocumentationContentHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
    }
}

package com.example.arbomachine.domain.treephoto.facade;

import com.example.arbomachine.domain.treephoto.dto.ProjectPhotoResponse;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoContent;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoDocumentSettingsRequest;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoResponse;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoUpload;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.DeleteTreePhotoHandler;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.GetProjectPhotosHandler;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.GetTreePhotoContentHandler;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.GetTreePhotosHandler;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.UpdateTreePhotoDocumentSettingsHandler;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.UploadTreePhotoHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Zdjecia drzew w projektach zalogowanego uzytkownika. Wlasnosc wynika z hierarchii
 * TreePhoto -> Tree -> Project -> owner. Tylko deleguje do handlerow z service/treephotofacade.
 */
@Component
public class TreePhotoFacade {
    private final UploadTreePhotoHandler uploadTreePhotoHandler;
    private final GetTreePhotosHandler getTreePhotosHandler;
    private final GetTreePhotoContentHandler getTreePhotoContentHandler;
    private final DeleteTreePhotoHandler deleteTreePhotoHandler;
    private final GetProjectPhotosHandler getProjectPhotosHandler;
    private final UpdateTreePhotoDocumentSettingsHandler updateTreePhotoDocumentSettingsHandler;

    public TreePhotoFacade(UploadTreePhotoHandler uploadTreePhotoHandler,
                           GetTreePhotosHandler getTreePhotosHandler,
                           GetTreePhotoContentHandler getTreePhotoContentHandler,
                           DeleteTreePhotoHandler deleteTreePhotoHandler,
                           GetProjectPhotosHandler getProjectPhotosHandler,
                           UpdateTreePhotoDocumentSettingsHandler updateTreePhotoDocumentSettingsHandler) {
        this.uploadTreePhotoHandler = uploadTreePhotoHandler;
        this.getTreePhotosHandler = getTreePhotosHandler;
        this.getTreePhotoContentHandler = getTreePhotoContentHandler;
        this.deleteTreePhotoHandler = deleteTreePhotoHandler;
        this.getProjectPhotosHandler = getProjectPhotosHandler;
        this.updateTreePhotoDocumentSettingsHandler = updateTreePhotoDocumentSettingsHandler;
    }

    public TreePhotoResponse uploadPhotoOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                            TreePhotoUpload treePhotoUpload) {
        return uploadTreePhotoHandler.handle(currentUserEmail, projectId, treeId, treePhotoUpload);
    }

    public List<TreePhotoResponse> getPhotosOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        return getTreePhotosHandler.handle(currentUserEmail, projectId, treeId);
    }

    public TreePhotoContent getPhotoContentOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                               Long treePhotoId) {
        return getTreePhotoContentHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
    }

    /** Etap 52: podpis zdjecia i wybor "w dokumencie". */
    public TreePhotoResponse updatePhotoDocumentSettingsOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                            Long treePhotoId,
                                                                            TreePhotoDocumentSettingsRequest request) {
        return updateTreePhotoDocumentSettingsHandler.handle(currentUserEmail, projectId, treeId, treePhotoId, request);
    }

    public void deletePhotoOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        deleteTreePhotoHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
    }

    public List<ProjectPhotoResponse> getPhotosOfProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        return getProjectPhotosHandler.handle(currentUserEmail, projectId);
    }
}

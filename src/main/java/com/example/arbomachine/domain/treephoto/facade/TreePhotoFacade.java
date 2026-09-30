package com.example.arbomachine.domain.treephoto.facade;

import com.example.arbomachine.domain.treephoto.dto.TreePhotoContent;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoResponse;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoUpload;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.DeleteTreePhotoHandler;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.GetTreePhotoContentHandler;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.GetTreePhotosHandler;
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

    public TreePhotoFacade(UploadTreePhotoHandler uploadTreePhotoHandler,
                           GetTreePhotosHandler getTreePhotosHandler,
                           GetTreePhotoContentHandler getTreePhotoContentHandler,
                           DeleteTreePhotoHandler deleteTreePhotoHandler) {
        this.uploadTreePhotoHandler = uploadTreePhotoHandler;
        this.getTreePhotosHandler = getTreePhotosHandler;
        this.getTreePhotoContentHandler = getTreePhotoContentHandler;
        this.deleteTreePhotoHandler = deleteTreePhotoHandler;
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

    public void deletePhotoOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId) {
        deleteTreePhotoHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
    }
}

package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.dto.TreeImportUpload;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportValidationResponse;
import com.example.arbomachine.domain.dataexchange.imports.TreeImportAnalyzer;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** "Sprawdz plik": odczyt i walidacja importu w kontekscie WLASNEGO projektu (404 dla cudzego) - nic nie zapisuje. */
@Component
public class ValidateTreeImportHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeImportAnalyzer treeImportAnalyzer;

    public ValidateTreeImportHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                     TreeImportAnalyzer treeImportAnalyzer) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeImportAnalyzer = treeImportAnalyzer;
    }

    @Transactional(readOnly = true)
    public TreeImportValidationResponse handle(String currentUserEmail, Long projectId, TreeImportUpload upload) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeImportAnalyzer.analyze(ownedProject, upload).response();
    }
}

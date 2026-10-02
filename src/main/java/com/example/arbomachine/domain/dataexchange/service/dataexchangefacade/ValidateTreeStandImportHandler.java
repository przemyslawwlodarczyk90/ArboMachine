package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.dto.TreeImportUpload;
import com.example.arbomachine.domain.dataexchange.dto.TreeStandImportValidationResponse;
import com.example.arbomachine.domain.dataexchange.imports.TreeStandImportAnalyzer;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** "Sprawdz plik" importu grup drzew (etap 29) - nic nie zapisuje. Cudzy projekt -> 404. */
@Component
public class ValidateTreeStandImportHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeStandImportAnalyzer treeStandImportAnalyzer;

    public ValidateTreeStandImportHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                          TreeStandImportAnalyzer treeStandImportAnalyzer) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeStandImportAnalyzer = treeStandImportAnalyzer;
    }

    @Transactional(readOnly = true)
    public TreeStandImportValidationResponse handle(String currentUserEmail, Long projectId, TreeImportUpload upload) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeStandImportAnalyzer.analyze(ownedProject, upload).response();
    }
}

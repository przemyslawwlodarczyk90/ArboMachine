package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.dto.TreeImportResultResponse;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportUpload;
import com.example.arbomachine.domain.dataexchange.exception.TreeImportRejectedException;
import com.example.arbomachine.domain.dataexchange.imports.TreeImportAnalyzer;
import com.example.arbomachine.domain.dataexchange.imports.TreeImportAnalyzer.TreeImportAnalysis;
import com.example.arbomachine.domain.dataexchange.imports.ValidatedTreeRow;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Import drzew do WLASNEGO projektu - "wszystko albo nic": plik jest sprawdzany ponownie (te same zasady co
 * "Sprawdz plik"); jakikolwiek blad -> 400 i nic nie jest zapisane. Kazdy wiersz tworzy NOWE drzewo (bez
 * aktualizacji istniejacych i bez scalania po numerze czy id z pliku). Jedna transakcja - blad zapisu cofa wszystko.
 * Id, projekt i wlasciciel nigdy nie pochodza z pliku.
 */
@Component
public class ImportTreesHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeImportAnalyzer treeImportAnalyzer;
    private final TreeRepository treeRepository;

    public ImportTreesHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                              TreeImportAnalyzer treeImportAnalyzer, TreeRepository treeRepository) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeImportAnalyzer = treeImportAnalyzer;
        this.treeRepository = treeRepository;
    }

    @Transactional
    public TreeImportResultResponse handle(String currentUserEmail, Long projectId, TreeImportUpload upload) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        TreeImportAnalysis analysis = treeImportAnalyzer.analyze(ownedProject, upload);
        if (!analysis.response().readyToImport()) throw new TreeImportRejectedException(analysis.response());

        Instant importTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
        for (ValidatedTreeRow row : analysis.validRows()) {
            Tree tree = Tree.createInProject(ownedProject, row.attributes(), importTime);
            if (row.locationOrNull() != null) tree.placeAt(row.locationOrNull(), importTime);
            treeRepository.save(tree);
        }
        return new TreeImportResultResponse(analysis.validRows().size(), analysis.response().warnings().size());
    }
}

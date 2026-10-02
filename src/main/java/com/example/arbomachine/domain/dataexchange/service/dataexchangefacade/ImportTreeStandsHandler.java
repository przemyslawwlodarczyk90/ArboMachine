package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.dto.TreeImportUpload;
import com.example.arbomachine.domain.dataexchange.dto.TreeStandImportResultResponse;
import com.example.arbomachine.domain.dataexchange.exception.TreeStandImportRejectedException;
import com.example.arbomachine.domain.dataexchange.imports.TreeStandImportAnalyzer;
import com.example.arbomachine.domain.dataexchange.imports.TreeStandImportAnalyzer.TreeStandImportAnalysis;
import com.example.arbomachine.domain.dataexchange.imports.TreeStandImportAnalyzer.ValidStand;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treestand.entity.TreeStand;
import com.example.arbomachine.domain.treestand.entity.TreeStandSource;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Import grup drzew (etap 29) - "wszystko albo nic", jak import drzew: plik sprawdzany ponownie, blad -> 400.
 * Kazdy obszar = NOWA grupa (zrodlo IMPORT); drzewa projektu lezace w obszarze sa przypisywane automatycznie.
 */
@Component
public class ImportTreeStandsHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeStandImportAnalyzer treeStandImportAnalyzer;
    private final TreeStandRepository treeStandRepository;
    private final ObjectMapper objectMapper;

    public ImportTreeStandsHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                   TreeStandImportAnalyzer treeStandImportAnalyzer, TreeStandRepository treeStandRepository,
                                   ObjectMapper objectMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeStandImportAnalyzer = treeStandImportAnalyzer;
        this.treeStandRepository = treeStandRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TreeStandImportResultResponse handle(String currentUserEmail, Long projectId, TreeImportUpload upload) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        TreeStandImportAnalysis analysis = treeStandImportAnalyzer.analyze(ownedProject, upload);
        if (!analysis.response().readyToImport()) throw new TreeStandImportRejectedException(analysis.response());
        Instant importTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
        int assignedTreeCount = 0;
        for (ValidStand stand : analysis.validStands()) {
            treeStandRepository.save(TreeStand.create(ownedProject, stand.name(), stand.notes(),
                    stand.polygon().toGeoJson(objectMapper).toString(), stand.polygon().roundedAreaSquareMeters(),
                    TreeStandSource.IMPORT, new java.util.HashSet<>(stand.treesInside()), importTime));
            assignedTreeCount += stand.treesInside().size();
        }
        return new TreeStandImportResultResponse(analysis.validStands().size(), assignedTreeCount);
    }
}

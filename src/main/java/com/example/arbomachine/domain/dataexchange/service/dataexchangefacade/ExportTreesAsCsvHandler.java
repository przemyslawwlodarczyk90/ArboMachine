package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportDataAssembler;
import com.example.arbomachine.domain.dataexchange.export.TreeCsvExporter;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Eksport drzew wlasnego projektu do CSV (";", UTF-8, naglowki po polsku). Cudzy / nieistniejacy projekt -> 404. */
@Component
public class ExportTreesAsCsvHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectExportDataAssembler projectExportDataAssembler;
    private final TreeCsvExporter treeCsvExporter;

    public ExportTreesAsCsvHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                  ProjectExportDataAssembler projectExportDataAssembler, TreeCsvExporter treeCsvExporter) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectExportDataAssembler = projectExportDataAssembler;
        this.treeCsvExporter = treeCsvExporter;
    }

    public ExportedFile handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeCsvExporter.export(projectExportDataAssembler.assemble(ownedProject, Instant.now()));
    }
}

package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportDataAssembler;
import com.example.arbomachine.domain.dataexchange.export.TreeXlsxExporter;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Eksport drzew wlasnego projektu do XLSX (arkusze "Drzewa" i "Projekt"). Cudzy / nieistniejacy projekt -> 404. */
@Component
public class ExportTreesAsXlsxHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectExportDataAssembler projectExportDataAssembler;
    private final TreeXlsxExporter treeXlsxExporter;

    public ExportTreesAsXlsxHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                  ProjectExportDataAssembler projectExportDataAssembler, TreeXlsxExporter treeXlsxExporter) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectExportDataAssembler = projectExportDataAssembler;
        this.treeXlsxExporter = treeXlsxExporter;
    }

    public ExportedFile handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeXlsxExporter.export(projectExportDataAssembler.assemble(ownedProject, Instant.now()));
    }
}

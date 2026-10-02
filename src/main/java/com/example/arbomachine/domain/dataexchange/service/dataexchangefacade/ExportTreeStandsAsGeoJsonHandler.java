package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportDataAssembler;
import com.example.arbomachine.domain.dataexchange.export.TreeStandsGeoJsonExporter;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Grupy drzew wlasnego projektu jako GeoJSON (etap 29). Cudzy projekt -> 404. */
@Component
public class ExportTreeStandsAsGeoJsonHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectExportDataAssembler projectExportDataAssembler;
    private final TreeStandsGeoJsonExporter treeStandsGeoJsonExporter;

    public ExportTreeStandsAsGeoJsonHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                     ProjectExportDataAssembler projectExportDataAssembler, TreeStandsGeoJsonExporter treeStandsGeoJsonExporter) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectExportDataAssembler = projectExportDataAssembler;
        this.treeStandsGeoJsonExporter = treeStandsGeoJsonExporter;
    }

    public ExportedFile handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeStandsGeoJsonExporter.export(projectExportDataAssembler.assemble(ownedProject, Instant.now()));
    }
}

package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportDataAssembler;
import com.example.arbomachine.domain.dataexchange.export.TreeGeoJsonExporter;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Eksport drzew z lokalizacja wlasnego projektu do GeoJSON (WGS 84, schemat V1). Cudzy / nieistniejacy projekt -> 404. */
@Component
public class ExportTreesAsGeoJsonHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectExportDataAssembler projectExportDataAssembler;
    private final TreeGeoJsonExporter treeGeoJsonExporter;

    public ExportTreesAsGeoJsonHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                  ProjectExportDataAssembler projectExportDataAssembler, TreeGeoJsonExporter treeGeoJsonExporter) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectExportDataAssembler = projectExportDataAssembler;
        this.treeGeoJsonExporter = treeGeoJsonExporter;
    }

    public ExportedFile handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeGeoJsonExporter.export(projectExportDataAssembler.assemble(ownedProject, Instant.now()));
    }
}

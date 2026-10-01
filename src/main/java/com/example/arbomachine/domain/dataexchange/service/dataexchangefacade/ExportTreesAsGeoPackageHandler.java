package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportDataAssembler;
import com.example.arbomachine.domain.dataexchange.export.TreeGeoPackageExporter;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Eksport drzew z lokalizacja wlasnego projektu do GeoPackage (warstwa "trees", POINT, EPSG:4326). Cudzy / nieistniejacy projekt -> 404. */
@Component
public class ExportTreesAsGeoPackageHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectExportDataAssembler projectExportDataAssembler;
    private final TreeGeoPackageExporter treeGeoPackageExporter;

    public ExportTreesAsGeoPackageHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                  ProjectExportDataAssembler projectExportDataAssembler, TreeGeoPackageExporter treeGeoPackageExporter) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectExportDataAssembler = projectExportDataAssembler;
        this.treeGeoPackageExporter = treeGeoPackageExporter;
    }

    public ExportedFile handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeGeoPackageExporter.export(projectExportDataAssembler.assemble(ownedProject, Instant.now()));
    }
}

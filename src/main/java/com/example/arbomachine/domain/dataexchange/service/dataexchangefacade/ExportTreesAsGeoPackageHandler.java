package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.crs.ExportCrsResolver;
import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportData;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportDataAssembler;
import com.example.arbomachine.domain.dataexchange.export.TreeGeoPackageExporter;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Eksport drzew z lokalizacja wlasnego projektu do GeoPackage (warstwy trees, tree_markings, stands + style QGIS).
 * Etap 34: uklad wybierany parametrem (brak = EPSG:4326). Cudzy / nieistniejacy projekt -> 404.
 */
@Component
public class ExportTreesAsGeoPackageHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectExportDataAssembler projectExportDataAssembler;
    private final TreeGeoPackageExporter treeGeoPackageExporter;
    private final ExportCrsResolver exportCrsResolver;

    public ExportTreesAsGeoPackageHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                  ProjectExportDataAssembler projectExportDataAssembler, TreeGeoPackageExporter treeGeoPackageExporter,
                  ExportCrsResolver exportCrsResolver) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectExportDataAssembler = projectExportDataAssembler;
        this.treeGeoPackageExporter = treeGeoPackageExporter;
        this.exportCrsResolver = exportCrsResolver;
    }

    public ExportedFile handle(String currentUserEmail, Long projectId, String requestedCrs) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        ProjectExportData data = projectExportDataAssembler.assemble(ownedProject, Instant.now());
        return treeGeoPackageExporter.export(data, exportCrsResolver.resolve(requestedCrs, data.trees()));
    }
}

package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.crs.ExportCrsResolver;
import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportData;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportDataAssembler;
import com.example.arbomachine.domain.dataexchange.export.QgisPackageExporter;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Etap 34: pakiet dla QGIS (ZIP: GeoPackage ze stylami + pliki QML + README) w wybranym ukladzie. Cudzy projekt -> 404. */
@Component
public class ExportQgisPackageHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectExportDataAssembler projectExportDataAssembler;
    private final ExportCrsResolver exportCrsResolver;
    private final QgisPackageExporter qgisPackageExporter;

    public ExportQgisPackageHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                    ProjectExportDataAssembler projectExportDataAssembler, ExportCrsResolver exportCrsResolver,
                                    QgisPackageExporter qgisPackageExporter) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectExportDataAssembler = projectExportDataAssembler;
        this.exportCrsResolver = exportCrsResolver;
        this.qgisPackageExporter = qgisPackageExporter;
    }

    public ExportedFile handle(String currentUserEmail, Long projectId, String requestedCrs) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        ProjectExportData data = projectExportDataAssembler.assemble(ownedProject, Instant.now());
        return qgisPackageExporter.export(data, exportCrsResolver.resolve(requestedCrs, data.trees()));
    }
}

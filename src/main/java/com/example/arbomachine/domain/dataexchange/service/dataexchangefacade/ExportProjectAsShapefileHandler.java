package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportDataAssembler;
import com.example.arbomachine.domain.dataexchange.export.ShapefileZipExporter;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Drzewa i grupy drzew wlasnego projektu jako Shapefile ZIP (etap 29). Cudzy projekt -> 404. */
@Component
public class ExportProjectAsShapefileHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectExportDataAssembler projectExportDataAssembler;
    private final ShapefileZipExporter shapefileZipExporter;

    public ExportProjectAsShapefileHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                     ProjectExportDataAssembler projectExportDataAssembler, ShapefileZipExporter shapefileZipExporter) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectExportDataAssembler = projectExportDataAssembler;
        this.shapefileZipExporter = shapefileZipExporter;
    }

    public ExportedFile handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return shapefileZipExporter.export(projectExportDataAssembler.assemble(ownedProject, Instant.now()));
    }
}

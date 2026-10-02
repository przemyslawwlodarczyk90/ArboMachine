package com.example.arbomachine.domain.dataexchange.service.dataexchangefacade;

import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.export.ProjectExportDataAssembler;
import com.example.arbomachine.domain.dataexchange.export.TreeWorksIcsExporter;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Kalendarz prac wlasnego projektu (.ics, etap 30). Cudzy / nieistniejacy projekt -> 404. */
@Component
public class ExportTreeWorksAsIcsHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectExportDataAssembler projectExportDataAssembler;
    private final TreeWorksIcsExporter treeWorksIcsExporter;

    public ExportTreeWorksAsIcsHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                       ProjectExportDataAssembler projectExportDataAssembler, TreeWorksIcsExporter treeWorksIcsExporter) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectExportDataAssembler = projectExportDataAssembler;
        this.treeWorksIcsExporter = treeWorksIcsExporter;
    }

    public ExportedFile handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeWorksIcsExporter.export(projectExportDataAssembler.assemble(ownedProject, Instant.now()));
    }
}

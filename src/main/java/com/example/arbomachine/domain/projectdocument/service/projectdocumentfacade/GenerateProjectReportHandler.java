package com.example.arbomachine.domain.projectdocument.service.projectdocumentfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.projectdocument.assembler.ProjectDocumentDataAssembler;
import com.example.arbomachine.domain.projectdocument.generator.DocumentGenerator;
import com.example.arbomachine.domain.projectdocument.generator.GeneratedDocument;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * "Dokument projektu" na zadanie (bez zapisu wygenerowanego pliku): wlasnosc projektu (cudzy/nieistniejacy -> 404),
 * dane z bazy (assembler, transakcja tylko do odczytu), renderowanie poza transakcja (generator).
 */
@Component
public class GenerateProjectReportHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectDocumentDataAssembler projectDocumentDataAssembler;
    private final DocumentGenerator documentGenerator;

    public GenerateProjectReportHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                        ProjectDocumentDataAssembler projectDocumentDataAssembler,
                                        DocumentGenerator documentGenerator) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectDocumentDataAssembler = projectDocumentDataAssembler;
        this.documentGenerator = documentGenerator;
    }

    public GeneratedDocument handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return documentGenerator.generate(projectDocumentDataAssembler.assemble(ownedProject, Instant.now()));
    }
}

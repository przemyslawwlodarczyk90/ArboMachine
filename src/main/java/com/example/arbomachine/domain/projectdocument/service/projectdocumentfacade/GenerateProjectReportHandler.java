package com.example.arbomachine.domain.projectdocument.service.projectdocumentfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.projectdocument.assembler.ProjectDocumentDataAssembler;
import com.example.arbomachine.domain.projectdocument.data.TreeCardScope;
import com.example.arbomachine.domain.projectdocument.generator.DocumentGenerationLimiter;
import com.example.arbomachine.domain.projectdocument.generator.DocumentGenerator;
import com.example.arbomachine.domain.projectdocument.generator.GeneratedDocument;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * "Dokument projektu" na zadanie (bez zapisu wygenerowanego pliku): wlasnosc projektu (cudzy/nieistniejacy -> 404),
 * dane z bazy (assembler, transakcja tylko do odczytu), renderowanie poza transakcja (generator). Najwyzej
 * app.project-documents.max-concurrent-generations generowan naraz (DocumentGenerationLimiter).
 */
@Component
public class GenerateProjectReportHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectDocumentDataAssembler projectDocumentDataAssembler;
    private final DocumentGenerator documentGenerator;
    private final DocumentGenerationLimiter documentGenerationLimiter;

    public GenerateProjectReportHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                        ProjectDocumentDataAssembler projectDocumentDataAssembler,
                                        DocumentGenerator documentGenerator,
                                        DocumentGenerationLimiter documentGenerationLimiter) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectDocumentDataAssembler = projectDocumentDataAssembler;
        this.documentGenerator = documentGenerator;
        this.documentGenerationLimiter = documentGenerationLimiter;
    }

    /** Etap 54: treeCardScope - ktore drzewa dostaja pelna karte. */
    public GeneratedDocument handle(String currentUserEmail, Long projectId, TreeCardScope treeCardScope) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        // Wlasnosc sprawdzona przed kolejka (cudzy projekt = 404 od razu); dane i renderowanie - jedno generowanie naraz.
        return documentGenerationLimiter.runLimited(() ->
                documentGenerator.generate(projectDocumentDataAssembler.assemble(ownedProject, Instant.now(), treeCardScope)));
    }
}

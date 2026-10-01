package com.example.arbomachine.domain.projectdocument.facade;

import com.example.arbomachine.domain.projectdocument.generator.GeneratedDocument;
import com.example.arbomachine.domain.projectdocument.service.projectdocumentfacade.GenerateProjectReportHandler;
import org.springframework.stereotype.Component;

/**
 * Dokumenty projektu (etap 22). Osobny modul jak podsumowanie projektu - czyta dane drzew, prac i zdjec, wiec moduly
 * domenowe nie zaleza od niego. Tylko deleguje do handlerow.
 */
@Component
public class ProjectDocumentFacade {
    private final GenerateProjectReportHandler generateProjectReportHandler;

    public ProjectDocumentFacade(GenerateProjectReportHandler generateProjectReportHandler) {
        this.generateProjectReportHandler = generateProjectReportHandler;
    }

    public GeneratedDocument generateProjectReportOfCurrentUser(String currentUserEmail, Long projectId) {
        return generateProjectReportHandler.handle(currentUserEmail, projectId);
    }
}

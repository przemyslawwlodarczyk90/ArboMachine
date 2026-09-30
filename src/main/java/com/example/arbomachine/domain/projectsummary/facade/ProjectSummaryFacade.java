package com.example.arbomachine.domain.projectsummary.facade;

import com.example.arbomachine.domain.projectsummary.dto.ProjectSummaryResponse;
import com.example.arbomachine.domain.projectsummary.service.projectsummaryfacade.GetProjectSummaryHandler;
import org.springframework.stereotype.Component;

/**
 * Podsumowanie projektu (read-model laczacy liczby z modulow drzew, zdjec i prac). Osobny modul, zeby modul
 * projektow nie zalezal od modulow, ktore same zaleza od niego. Tylko deleguje do handlerow.
 */
@Component
public class ProjectSummaryFacade {
    private final GetProjectSummaryHandler getProjectSummaryHandler;

    public ProjectSummaryFacade(GetProjectSummaryHandler getProjectSummaryHandler) {
        this.getProjectSummaryHandler = getProjectSummaryHandler;
    }

    public ProjectSummaryResponse getSummaryOfProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        return getProjectSummaryHandler.handle(currentUserEmail, projectId);
    }
}

package com.example.arbomachine.infrastructure.api.projectsummary;

import com.example.arbomachine.domain.projectsummary.dto.ProjectSummaryResponse;
import com.example.arbomachine.domain.projectsummary.facade.ProjectSummaryFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Podsumowanie projektu. Wymaga zalogowania (SecurityConfig: /api/projects/**); wlasnosc projektu sprawdza
 * ProjectSummaryFacade (handler) - cudzy albo nieistniejacy projekt daje 404, jak w API projektow.
 */
@RestController
@Tag(name = "Podsumowanie projektu", description = "Liczby drzew, lokalizacji, zdjec i prac w projekcie")
public class ProjectSummaryController {

    private final ProjectSummaryFacade projectSummaryFacade;

    public ProjectSummaryController(ProjectSummaryFacade projectSummaryFacade) {
        this.projectSummaryFacade = projectSummaryFacade;
    }

    @Operation(summary = "Podsumowanie wlasnego projektu (liczby liczone w bazie); cudzy projekt daje 404")
    @GetMapping("/api/projects/{projectId}/summary")
    public ResponseEntity<ProjectSummaryResponse> getProjectSummary(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                    @PathVariable Long projectId) {
        return ResponseEntity.ok(projectSummaryFacade.getSummaryOfProjectOfCurrentUser(authenticatedUser.getUsername(),
                projectId));
    }
}

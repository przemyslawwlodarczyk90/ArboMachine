package com.example.arbomachine.infrastructure.api.project;

import com.example.arbomachine.domain.project.dto.CreateProjectRequest;
import com.example.arbomachine.domain.project.dto.ProjectResponse;
import com.example.arbomachine.domain.project.dto.UpdateProjectRequest;
import com.example.arbomachine.domain.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Projekty zalogowanego uzytkownika. Wymaga zalogowania (SecurityConfig: /api/projects/**).
 * Kontroler nie sprawdza wlasnosci sam - przekazuje e-mail z tokenu do ProjectService,
 * ktory pobiera projekty wylacznie w kontekscie wlasciciela.
 */
@RestController
@RequestMapping("/api/projects")
@Validated
@Tag(name = "Projekty", description = "Projekty zalogowanego uzytkownika (tylko wlasne)")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(summary = "Utworzenie projektu - wlascicielem zostaje zalogowany uzytkownik")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                         @Valid @RequestBody CreateProjectRequest createProjectRequest) {
        ProjectResponse createdProject = projectService.createProjectForCurrentUser(authenticatedUser.getUsername(),
                createProjectRequest);
        return ResponseEntity.created(URI.create("/api/projects/" + createdProject.id())).body(createdProject);
    }

    @Operation(summary = "Lista projektow zalogowanego uzytkownika (najnowsze pierwsze)")
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjectsOfCurrentUser(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(projectService.getProjectsOfCurrentUser(authenticatedUser.getUsername()));
    }

    @Operation(summary = "Projekt po id - tylko wlasny; cudzy albo nieistniejacy daje 404")
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectOfCurrentUser(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                   @PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProjectOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Zmiana nazwy i/lub opisu wlasnego projektu (pola pominiete zostaja bez zmian)")
    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProjectOfCurrentUser(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                      @PathVariable Long projectId,
                                                                      @Valid @RequestBody UpdateProjectRequest updateProjectRequest) {
        return ResponseEntity.ok(projectService.updateProjectOfCurrentUser(authenticatedUser.getUsername(), projectId,
                updateProjectRequest));
    }
}

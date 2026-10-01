package com.example.arbomachine.infrastructure.api.projectdocument;

import com.example.arbomachine.domain.projectdocument.facade.ProjectDocumentFacade;
import com.example.arbomachine.domain.projectdocument.generator.GeneratedDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dokumenty projektu generowane na zadanie z aktualnych danych (bez przechowywania plikow i historii wersji).
 * Wymaga zalogowania (SecurityConfig: /api/projects/**); cudzy albo nieistniejacy projekt -> 404.
 */
@RestController
@Tag(name = "Dokumenty projektu", description = "Dokument PDF z aktualnych danych projektu (wersja MVP)")
public class ProjectDocumentController {

    private final ProjectDocumentFacade projectDocumentFacade;

    public ProjectDocumentController(ProjectDocumentFacade projectDocumentFacade) {
        this.projectDocumentFacade = projectDocumentFacade;
    }

    @Operation(summary = "\"Dokument projektu\" (PDF): dane projektu, tabela i karty drzew, prace, zdjecia")
    @GetMapping("/api/projects/{projectId}/documents/project-report")
    public ResponseEntity<byte[]> getProjectReport(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                   @PathVariable Long projectId) {
        GeneratedDocument document = projectDocumentFacade.generateProjectReportOfCurrentUser(
                authenticatedUser.getUsername(), projectId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(document.fileName()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(document.content());
    }
}

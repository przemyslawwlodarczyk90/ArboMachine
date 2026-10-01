package com.example.arbomachine.infrastructure.api.treephoto;

import com.example.arbomachine.domain.treephoto.dto.ProjectPhotoResponse;
import com.example.arbomachine.domain.treephoto.facade.TreePhotoFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Zdjecia wszystkich drzew projektu (materialy do dokumentacji) jednym zapytaniem; cudzy projekt -> 404. */
@RestController
@Tag(name = "Zdjecia drzew", description = "Zdjecia drzew w projektach zalogowanego uzytkownika (JPEG, PNG, WebP)")
public class ProjectPhotosController {

    private final TreePhotoFacade treePhotoFacade;

    public ProjectPhotosController(TreePhotoFacade treePhotoFacade) {
        this.treePhotoFacade = treePhotoFacade;
    }

    @Operation(summary = "Zdjecia wszystkich drzew wlasnego projektu z numerem drzewa i informacja o adnotacji")
    @GetMapping("/api/projects/{projectId}/photos")
    public ResponseEntity<List<ProjectPhotoResponse>> getPhotosOfProject(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                         @PathVariable Long projectId) {
        return ResponseEntity.ok(treePhotoFacade.getPhotosOfProjectOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }
}

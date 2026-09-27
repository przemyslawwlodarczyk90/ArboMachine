package com.example.arbomachine.infrastructure.api.tree;

import com.example.arbomachine.domain.tree.dto.CreateTreeRequest;
import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.service.TreeService;
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
 * Drzewa w projekcie zalogowanego uzytkownika. Wymaga zalogowania (SecurityConfig: /api/projects/**).
 * Id projektu pochodzi wylacznie z URL; wlasnosc projektu sprawdza TreeService przed kazda operacja.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/trees")
@Validated
@Tag(name = "Drzewa", description = "Drzewa w projektach zalogowanego uzytkownika (tylko wlasne projekty)")
public class TreeController {

    private final TreeService treeService;

    public TreeController(TreeService treeService) {
        this.treeService = treeService;
    }

    @Operation(summary = "Dodanie drzewa do wlasnego projektu; cudzy albo nieistniejacy projekt daje 404")
    @PostMapping
    public ResponseEntity<TreeResponse> createTree(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                   @PathVariable Long projectId,
                                                   @Valid @RequestBody CreateTreeRequest createTreeRequest) {
        TreeResponse createdTree = treeService.createTreeInProjectOfCurrentUser(authenticatedUser.getUsername(),
                projectId, createTreeRequest);
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/trees/" + createdTree.id()))
                .body(createdTree);
    }

    @Operation(summary = "Drzewa wlasnego projektu w kolejnosci dodawania")
    @GetMapping
    public ResponseEntity<List<TreeResponse>> getTreesOfProject(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                @PathVariable Long projectId) {
        return ResponseEntity.ok(treeService.getTreesOfProjectOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Drzewo po id - tylko z tego (wlasnego) projektu; w innym przypadku 404")
    @GetMapping("/{treeId}")
    public ResponseEntity<TreeResponse> getTreeOfProject(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                         @PathVariable Long projectId,
                                                         @PathVariable Long treeId) {
        return ResponseEntity.ok(treeService.getTreeOfProjectOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId));
    }
}

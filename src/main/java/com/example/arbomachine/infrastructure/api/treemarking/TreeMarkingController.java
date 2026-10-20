package com.example.arbomachine.infrastructure.api.treemarking;

import com.example.arbomachine.domain.treemarking.dto.TreeMarkingDefinitionResponse;
import com.example.arbomachine.domain.treemarking.dto.TreeMarkingsRequest;
import com.example.arbomachine.domain.treemarking.dto.TreeMarkingsResponse;
import com.example.arbomachine.domain.treemarking.facade.TreeMarkingFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Oznaczenia drzew (etap 31): wiele symboli graficznych na jednym drzewie (wartosciowe, do usuniecia, zlom...). */
@RestController
@Validated
@Tag(name = "Oznaczenia drzew", description = "Katalog oznaczen V1 i oznaczenia drzew w projekcie zalogowanego uzytkownika")
public class TreeMarkingController {

    private final TreeMarkingFacade treeMarkingFacade;

    public TreeMarkingController(TreeMarkingFacade treeMarkingFacade) {
        this.treeMarkingFacade = treeMarkingFacade;
    }

    @Operation(summary = "Katalog oznaczen (kod, nazwa, grupa, skrot na mape, kolor)")
    @GetMapping("/api/tree-markings")
    public ResponseEntity<List<TreeMarkingDefinitionResponse>> getCatalog() {
        return ResponseEntity.ok(treeMarkingFacade.getCatalog());
    }

    @Operation(summary = "Oznaczenia wszystkich drzew projektu (tylko drzewa z oznaczeniami)")
    @GetMapping("/api/projects/{projectId}/tree-markings")
    public ResponseEntity<List<TreeMarkingsResponse>> getProjectMarkings(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                         @PathVariable Long projectId) {
        return ResponseEntity.ok(treeMarkingFacade.getMarkingsOfProjectOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Oznaczenia drzewa")
    @GetMapping("/api/projects/{projectId}/trees/{treeId}/markings")
    public ResponseEntity<TreeMarkingsResponse> getTreeMarkings(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                @PathVariable Long projectId, @PathVariable Long treeId) {
        return ResponseEntity.ok(treeMarkingFacade.getMarkingsOfTreeOfCurrentUser(authenticatedUser.getUsername(), projectId, treeId));
    }

    @Operation(summary = "Komplet oznaczen drzewa (zastepuje poprzednie; pusta lista usuwa wszystkie)")
    @PutMapping("/api/projects/{projectId}/trees/{treeId}/markings")
    public ResponseEntity<TreeMarkingsResponse> setTreeMarkings(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                @PathVariable Long projectId, @PathVariable Long treeId,
                                                                @Valid @RequestBody TreeMarkingsRequest request) {
        return ResponseEntity.ok(treeMarkingFacade.setMarkingsOfTreeOfCurrentUser(authenticatedUser.getUsername(), projectId, treeId, request));
    }
}

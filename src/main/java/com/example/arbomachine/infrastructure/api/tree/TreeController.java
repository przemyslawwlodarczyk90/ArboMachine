package com.example.arbomachine.infrastructure.api.tree;

import com.example.arbomachine.domain.mapsymbol.dto.MapSymbolRequest;
import com.example.arbomachine.domain.tree.dto.CreateTreeRequest;
import com.example.arbomachine.domain.tree.dto.TreeLocationRequest;
import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.dto.UpdateTreeRequest;
import com.example.arbomachine.domain.tree.facade.TreeFacade;
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
 * Id projektu pochodzi wylacznie z URL; wlasnosc projektu sprawdza TreeFacade (handlery) przed kazda operacja.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/trees")
@Validated
@Tag(name = "Drzewa", description = "Drzewa w projektach zalogowanego uzytkownika (tylko wlasne projekty)")
public class TreeController {

    private final TreeFacade treeFacade;

    public TreeController(TreeFacade treeFacade) {
        this.treeFacade = treeFacade;
    }

    @Operation(summary = "Dodanie drzewa do wlasnego projektu; cudzy albo nieistniejacy projekt daje 404")
    @PostMapping
    public ResponseEntity<TreeResponse> createTree(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                   @PathVariable Long projectId,
                                                   @Valid @RequestBody CreateTreeRequest createTreeRequest,
                                                   @RequestParam(value = "clientRequestId", required = false) String clientRequestId) {
        TreeResponse createdTree = treeFacade.createTreeInProjectOfCurrentUser(authenticatedUser.getUsername(),
                projectId, createTreeRequest, clientRequestId);
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/trees/" + createdTree.id()))
                .body(createdTree);
    }

    @Operation(summary = "Drzewa wlasnego projektu w kolejnosci dodawania")
    @GetMapping
    public ResponseEntity<List<TreeResponse>> getTreesOfProject(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                @PathVariable Long projectId) {
        return ResponseEntity.ok(treeFacade.getTreesOfProjectOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Drzewo po id - tylko z tego (wlasnego) projektu; w innym przypadku 404")
    @GetMapping("/{treeId}")
    public ResponseEntity<TreeResponse> getTreeOfProject(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                         @PathVariable Long projectId,
                                                         @PathVariable Long treeId) {
        return ResponseEntity.ok(treeFacade.getTreeOfProjectOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId));
    }

    @Operation(summary = "Edycja drzewa wlasnego projektu - request zawiera komplet edytowalnych pol; "
            + "drzewo z innego projektu albo cudzy projekt daje 404")
    @PatchMapping("/{treeId}")
    public ResponseEntity<TreeResponse> updateTreeOfProject(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                            @PathVariable Long projectId,
                                                            @PathVariable Long treeId,
                                                            @Valid @RequestBody UpdateTreeRequest updateTreeRequest) {
        return ResponseEntity.ok(treeFacade.updateTreeOfProjectOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, updateTreeRequest));
    }

    @Operation(summary = "Ustawienie albo zmiana lokalizacji drzewa (latitude/longitude WGS84, obie wymagane); "
            + "drzewo z innego projektu albo cudzy projekt daje 404")
    @PutMapping("/{treeId}/location")
    public ResponseEntity<TreeResponse> setTreeLocation(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                        @PathVariable Long projectId,
                                                        @PathVariable Long treeId,
                                                        @Valid @RequestBody TreeLocationRequest treeLocationRequest) {
        return ResponseEntity.ok(treeFacade.setLocationOfTreeOfProjectOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, treeLocationRequest));
    }

    @Operation(summary = "Usuniecie lokalizacji drzewa (drzewo wraca do stanu bez lokalizacji); zwraca drzewo")
    @DeleteMapping("/{treeId}/location")
    public ResponseEntity<TreeResponse> clearTreeLocation(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                          @PathVariable Long projectId,
                                                          @PathVariable Long treeId) {
        return ResponseEntity.ok(treeFacade.clearLocationOfTreeOfProjectOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId));
    }

    @Operation(summary = "Ustawienie albo zmiana symbolu drzewa na mapie (kod z GET /api/map-symbols); zwraca drzewo")
    @PutMapping("/{treeId}/map-symbol")
    public ResponseEntity<TreeResponse> setTreeMapSymbol(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                         @PathVariable Long projectId,
                                                         @PathVariable Long treeId,
                                                         @Valid @RequestBody MapSymbolRequest mapSymbolRequest) {
        return ResponseEntity.ok(treeFacade.setMapSymbolOfTreeOfProjectOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, mapSymbolRequest));
    }

    @Operation(summary = "Usuniecie symbolu drzewa (neutralny marker); zwraca drzewo")
    @DeleteMapping("/{treeId}/map-symbol")
    public ResponseEntity<TreeResponse> clearTreeMapSymbol(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                           @PathVariable Long projectId,
                                                           @PathVariable Long treeId) {
        return ResponseEntity.ok(treeFacade.clearMapSymbolOfTreeOfProjectOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId));
    }

    @Operation(summary = "Usuniecie drzewa (etap 44) razem ze zdjeciami, pracami, waloryzacja, kontrolami i oznaczeniami; 204")
    @DeleteMapping("/{treeId}")
    public ResponseEntity<Void> deleteTree(@AuthenticationPrincipal UserDetails authenticatedUser,
                                           @PathVariable Long projectId,
                                           @PathVariable Long treeId) {
        treeFacade.deleteTreeOfProjectOfCurrentUser(authenticatedUser.getUsername(), projectId, treeId);
        return ResponseEntity.noContent().build();
    }
}

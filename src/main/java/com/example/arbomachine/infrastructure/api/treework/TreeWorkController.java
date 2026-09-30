package com.example.arbomachine.infrastructure.api.treework;

import com.example.arbomachine.domain.mapsymbol.dto.MapSymbolRequest;
import com.example.arbomachine.domain.treework.dto.CreateTreeWorkRequest;
import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.dto.UpdateTreeWorkRequest;
import com.example.arbomachine.domain.treework.facade.TreeWorkFacade;
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
 * Zalecenia / prace przy drzewie. Wymaga zalogowania (SecurityConfig: /api/projects/**). Id projektu, drzewa
 * i pracy pochodza wylacznie z URL; cala hierarchie (wlasny projekt -> drzewo w projekcie -> praca w drzewie)
 * sprawdza TreeWorkFacade (handlery) przed kazda operacja.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/trees/{treeId}/works")
@Validated
@Tag(name = "Zalecenia i prace", description = "Zalecenia/prace przy drzewach w projektach zalogowanego uzytkownika")
public class TreeWorkController {

    private final TreeWorkFacade treeWorkFacade;

    public TreeWorkController(TreeWorkFacade treeWorkFacade) {
        this.treeWorkFacade = treeWorkFacade;
    }

    @Operation(summary = "Dodanie pracy do drzewa (nazwa wymagana, opis opcjonalny); cudzy projekt/drzewo daje 404")
    @PostMapping
    public ResponseEntity<TreeWorkResponse> createTreeWork(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                           @PathVariable Long projectId,
                                                           @PathVariable Long treeId,
                                                           @Valid @RequestBody CreateTreeWorkRequest createTreeWorkRequest) {
        TreeWorkResponse createdTreeWork = treeWorkFacade.createWorkForTreeOfCurrentUser(
                authenticatedUser.getUsername(), projectId, treeId, createTreeWorkRequest);
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/trees/" + treeId + "/works/"
                + createdTreeWork.id())).body(createdTreeWork);
    }

    @Operation(summary = "Prace drzewa w kolejnosci dodawania")
    @GetMapping
    public ResponseEntity<List<TreeWorkResponse>> getTreeWorks(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                               @PathVariable Long projectId,
                                                               @PathVariable Long treeId) {
        return ResponseEntity.ok(treeWorkFacade.getWorksOfTreeOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId));
    }

    @Operation(summary = "Praca po id - tylko z drzewa z URL; praca z innego drzewa daje 404")
    @GetMapping("/{workId}")
    public ResponseEntity<TreeWorkResponse> getTreeWork(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                        @PathVariable Long projectId,
                                                        @PathVariable Long treeId,
                                                        @PathVariable Long workId) {
        return ResponseEntity.ok(treeWorkFacade.getWorkOfTreeOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, workId));
    }

    @Operation(summary = "Edycja pracy - komplet pol (pominiety/pusty opis = wyczyszczenie); innego drzewa daje 404")
    @PatchMapping("/{workId}")
    public ResponseEntity<TreeWorkResponse> updateTreeWork(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                           @PathVariable Long projectId,
                                                           @PathVariable Long treeId,
                                                           @PathVariable Long workId,
                                                           @Valid @RequestBody UpdateTreeWorkRequest updateTreeWorkRequest) {
        return ResponseEntity.ok(treeWorkFacade.updateWorkOfTreeOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, workId, updateTreeWorkRequest));
    }

    @Operation(summary = "Usuniecie pracy; praca z innego drzewa albo cudzy projekt daje 404")
    @DeleteMapping("/{workId}")
    public ResponseEntity<Void> deleteTreeWork(@AuthenticationPrincipal UserDetails authenticatedUser,
                                               @PathVariable Long projectId,
                                               @PathVariable Long treeId,
                                               @PathVariable Long workId) {
        treeWorkFacade.deleteWorkOfTreeOfCurrentUser(authenticatedUser.getUsername(), projectId, treeId, workId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Ustawienie albo zmiana symbolu pracy na mapie (bez edycji nazwy/opisu); zwraca prace")
    @PutMapping("/{workId}/map-symbol")
    public ResponseEntity<TreeWorkResponse> setTreeWorkMapSymbol(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                 @PathVariable Long projectId,
                                                                 @PathVariable Long treeId,
                                                                 @PathVariable Long workId,
                                                                 @Valid @RequestBody MapSymbolRequest mapSymbolRequest) {
        return ResponseEntity.ok(treeWorkFacade.setMapSymbolOfWorkOfTreeOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, workId, mapSymbolRequest));
    }

    @Operation(summary = "Usuniecie symbolu pracy z mapy; zwraca prace")
    @DeleteMapping("/{workId}/map-symbol")
    public ResponseEntity<TreeWorkResponse> clearTreeWorkMapSymbol(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                   @PathVariable Long projectId,
                                                                   @PathVariable Long treeId,
                                                                   @PathVariable Long workId) {
        return ResponseEntity.ok(treeWorkFacade.clearMapSymbolOfWorkOfTreeOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, workId));
    }
}

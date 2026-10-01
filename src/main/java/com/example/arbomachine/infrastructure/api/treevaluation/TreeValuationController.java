package com.example.arbomachine.infrastructure.api.treevaluation;

import com.example.arbomachine.domain.treevaluation.dto.TreeValuationRequest;
import com.example.arbomachine.domain.treevaluation.dto.TreeValuationResponse;
import com.example.arbomachine.domain.treevaluation.facade.TreeValuationFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Waloryzacja drzew - wylacznie przez Project -> Tree -> Valuation (SecurityConfig: /api/projects/**, wlasnosc
 * sprawdzaja handlery; cudzy projekt albo drzewo spoza projektu -> 404). Zapis oceny arborysty, bez obliczen.
 */
@RestController
@Tag(name = "Waloryzacja drzew", description = "Ocena arborysty: kategoria ogolna V1 i opisowe wartosci skladowe (bez algorytmu)")
public class TreeValuationController {

    private final TreeValuationFacade treeValuationFacade;

    public TreeValuationController(TreeValuationFacade treeValuationFacade) {
        this.treeValuationFacade = treeValuationFacade;
    }

    @Operation(summary = "Waloryzacja drzewa; brak waloryzacji -> 200 z hasValuation=false")
    @GetMapping("/api/projects/{projectId}/trees/{treeId}/valuation")
    public ResponseEntity<TreeValuationResponse> getValuation(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                              @PathVariable Long projectId, @PathVariable Long treeId) {
        return ResponseEntity.ok(treeValuationFacade.getValuationOfTreeOfCurrentUser(authenticatedUser.getUsername(), projectId, treeId));
    }

    @Operation(summary = "Zapis waloryzacji (utworz albo zastap cala ocene); pusta ocena -> 400")
    @PutMapping("/api/projects/{projectId}/trees/{treeId}/valuation")
    public ResponseEntity<TreeValuationResponse> saveValuation(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                               @PathVariable Long projectId, @PathVariable Long treeId,
                                                               @Valid @RequestBody TreeValuationRequest request) {
        return ResponseEntity.ok(treeValuationFacade.saveValuationOfTreeOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, request));
    }

    @Operation(summary = "Usuniecie waloryzacji drzewa (brak waloryzacji tez daje 204)")
    @DeleteMapping("/api/projects/{projectId}/trees/{treeId}/valuation")
    public ResponseEntity<Void> deleteValuation(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                @PathVariable Long projectId, @PathVariable Long treeId) {
        treeValuationFacade.deleteValuationOfTreeOfCurrentUser(authenticatedUser.getUsername(), projectId, treeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Waloryzacje wszystkich drzew projektu (tylko drzewa z waloryzacja)")
    @GetMapping("/api/projects/{projectId}/valuations")
    public ResponseEntity<List<TreeValuationResponse>> getValuationsOfProject(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                              @PathVariable Long projectId) {
        return ResponseEntity.ok(treeValuationFacade.getValuationsOfProjectOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }
}

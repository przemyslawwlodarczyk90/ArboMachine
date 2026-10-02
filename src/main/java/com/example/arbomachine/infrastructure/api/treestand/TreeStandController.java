package com.example.arbomachine.infrastructure.api.treestand;

import com.example.arbomachine.domain.treestand.dto.TreeStandRequest;
import com.example.arbomachine.domain.treestand.dto.TreeStandResponse;
import com.example.arbomachine.domain.treestand.facade.TreeStandFacade;
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

/** Grupy drzew projektu (etap 29) - obszary (GeoJSON Polygon, WGS 84) z przypisanymi drzewami. Wlasnosc: projekt. */
@RestController
@RequestMapping("/api/projects/{projectId}/stands")
@Validated
@Tag(name = "Grupy drzew", description = "Obszary (wielokaty) grup drzew w projekcie zalogowanego uzytkownika")
public class TreeStandController {

    private final TreeStandFacade treeStandFacade;

    public TreeStandController(TreeStandFacade treeStandFacade) {
        this.treeStandFacade = treeStandFacade;
    }

    @Operation(summary = "Grupy drzew projektu (wg nazwy) z geometria i id drzew")
    @GetMapping
    public ResponseEntity<List<TreeStandResponse>> getStands(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                             @PathVariable Long projectId) {
        return ResponseEntity.ok(treeStandFacade.getStandsOfProjectOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Nowa grupa: nazwa, uwagi, geometria (GeoJSON Polygon), treeIds; assignTreesInside dopisuje drzewa w obszarze")
    @PostMapping
    public ResponseEntity<TreeStandResponse> createStand(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                         @PathVariable Long projectId, @Valid @RequestBody TreeStandRequest request) {
        TreeStandResponse created = treeStandFacade.createStandOfCurrentUser(authenticatedUser.getUsername(), projectId, request);
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/stands/" + created.id())).body(created);
    }

    @Operation(summary = "Grupa drzew po id (grupa innego projektu: 404)")
    @GetMapping("/{standId}")
    public ResponseEntity<TreeStandResponse> getStand(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                      @PathVariable Long projectId, @PathVariable Long standId) {
        return ResponseEntity.ok(treeStandFacade.getStandOfCurrentUser(authenticatedUser.getUsername(), projectId, standId));
    }

    @Operation(summary = "Edycja grupy - komplet danych (nazwa, uwagi, geometria, drzewa)")
    @PutMapping("/{standId}")
    public ResponseEntity<TreeStandResponse> updateStand(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                         @PathVariable Long projectId, @PathVariable Long standId,
                                                         @Valid @RequestBody TreeStandRequest request) {
        return ResponseEntity.ok(treeStandFacade.updateStandOfCurrentUser(authenticatedUser.getUsername(), projectId, standId, request));
    }

    @Operation(summary = "Usuniecie grupy (drzewa zostaja)")
    @DeleteMapping("/{standId}")
    public ResponseEntity<Void> deleteStand(@AuthenticationPrincipal UserDetails authenticatedUser,
                                            @PathVariable Long projectId, @PathVariable Long standId) {
        treeStandFacade.deleteStandOfCurrentUser(authenticatedUser.getUsername(), projectId, standId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Przypisanie drzew, ktorych punkt lezy w obszarze grupy (nic nie usuwa)")
    @PostMapping("/{standId}/assign-trees-inside")
    public ResponseEntity<TreeStandResponse> assignTreesInside(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                               @PathVariable Long projectId, @PathVariable Long standId) {
        return ResponseEntity.ok(treeStandFacade.assignTreesInsideStandOfCurrentUser(authenticatedUser.getUsername(), projectId, standId));
    }
}

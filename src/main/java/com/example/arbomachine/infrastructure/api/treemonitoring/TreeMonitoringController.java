package com.example.arbomachine.infrastructure.api.treemonitoring;

import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordRequest;
import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordResponse;
import com.example.arbomachine.domain.treemonitoring.facade.TreeMonitoringFacade;
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
 * Monitoring stanu drzewa (etap 29). Wymaga zalogowania (SecurityConfig: /api/projects/**). Id projektu, drzewa
 * i wpisu pochodza wylacznie z URL; cala hierarchie sprawdza TreeMonitoringFacade (handlery) przed kazda operacja.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/trees/{treeId}/monitoring")
@Validated
@Tag(name = "Monitoring stanu drzew", description = "Historia kontroli stanu drzew (ocena arborysty, bez obliczen)")
public class TreeMonitoringController {

    private final TreeMonitoringFacade treeMonitoringFacade;

    public TreeMonitoringController(TreeMonitoringFacade treeMonitoringFacade) {
        this.treeMonitoringFacade = treeMonitoringFacade;
    }

    @Operation(summary = "Historia kontroli drzewa - najnowsza pierwsza; pusta lista = drzewo nie sprawdzone")
    @GetMapping
    public ResponseEntity<List<TreeMonitoringRecordResponse>> getRecords(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                         @PathVariable Long projectId, @PathVariable Long treeId) {
        return ResponseEntity.ok(treeMonitoringFacade.getRecordsOfTreeOfCurrentUser(authenticatedUser.getUsername(), projectId, treeId));
    }

    @Operation(summary = "Nowy wpis: data kontroli (wymagana), ocena stanu GOOD/FAIR/POOR (opcjonalna), uwagi; POOR wysyla powiadomienie")
    @PostMapping
    public ResponseEntity<TreeMonitoringRecordResponse> createRecord(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                     @PathVariable Long projectId, @PathVariable Long treeId,
                                                                     @Valid @RequestBody TreeMonitoringRecordRequest request) {
        TreeMonitoringRecordResponse createdRecord = treeMonitoringFacade.createRecordForTreeOfCurrentUser(
                authenticatedUser.getUsername(), projectId, treeId, request);
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/trees/" + treeId + "/monitoring/"
                + createdRecord.id())).body(createdRecord);
    }

    @Operation(summary = "Wpis po id - tylko z drzewa z URL; wpis innego drzewa daje 404")
    @GetMapping("/{recordId}")
    public ResponseEntity<TreeMonitoringRecordResponse> getRecord(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                  @PathVariable Long projectId, @PathVariable Long treeId,
                                                                  @PathVariable Long recordId) {
        return ResponseEntity.ok(treeMonitoringFacade.getRecordOfTreeOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, recordId));
    }

    @Operation(summary = "Edycja wpisu - komplet pol (pominiete pole = brak wartosci)")
    @PutMapping("/{recordId}")
    public ResponseEntity<TreeMonitoringRecordResponse> updateRecord(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                     @PathVariable Long projectId, @PathVariable Long treeId,
                                                                     @PathVariable Long recordId,
                                                                     @Valid @RequestBody TreeMonitoringRecordRequest request) {
        return ResponseEntity.ok(treeMonitoringFacade.updateRecordOfTreeOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, recordId, request));
    }

    @Operation(summary = "Usuniecie wpisu; wpis innego drzewa albo cudzy projekt daje 404")
    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteRecord(@AuthenticationPrincipal UserDetails authenticatedUser,
                                             @PathVariable Long projectId, @PathVariable Long treeId,
                                             @PathVariable Long recordId) {
        treeMonitoringFacade.deleteRecordOfTreeOfCurrentUser(authenticatedUser.getUsername(), projectId, treeId, recordId);
        return ResponseEntity.noContent().build();
    }
}

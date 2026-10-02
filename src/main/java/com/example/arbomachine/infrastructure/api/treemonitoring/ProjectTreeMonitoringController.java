package com.example.arbomachine.infrastructure.api.treemonitoring;

import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordResponse;
import com.example.arbomachine.domain.treemonitoring.facade.TreeMonitoringFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Ostatnia kontrola kazdego drzewa projektu jednym zapytaniem - lista drzew bez N+1. */
@RestController
@Tag(name = "Monitoring stanu drzew")
public class ProjectTreeMonitoringController {

    private final TreeMonitoringFacade treeMonitoringFacade;

    public ProjectTreeMonitoringController(TreeMonitoringFacade treeMonitoringFacade) {
        this.treeMonitoringFacade = treeMonitoringFacade;
    }

    @Operation(summary = "Ostatnia kontrola kazdego drzewa wlasnego projektu (drzewa bez kontroli pominiete)")
    @GetMapping("/api/projects/{projectId}/monitoring/latest")
    public ResponseEntity<List<TreeMonitoringRecordResponse>> getLatestRecordsOfProject(
            @AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId) {
        return ResponseEntity.ok(treeMonitoringFacade.getLatestRecordsOfProjectOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }
}

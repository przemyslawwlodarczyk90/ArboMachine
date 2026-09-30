package com.example.arbomachine.infrastructure.api.treework;

import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.facade.TreeWorkFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Prace wszystkich drzew projektu jednym zapytaniem - dla mapy projektu (symbole prac, legenda) bez N+1.
 * Wymaga zalogowania (SecurityConfig: /api/projects/**); cudzy projekt -> 404.
 */
@RestController
@Tag(name = "Zalecenia i prace", description = "Zalecenia/prace przy drzewach w projektach zalogowanego uzytkownika")
public class ProjectTreeWorksController {

    private final TreeWorkFacade treeWorkFacade;

    public ProjectTreeWorksController(TreeWorkFacade treeWorkFacade) {
        this.treeWorkFacade = treeWorkFacade;
    }

    @Operation(summary = "Prace wszystkich drzew wlasnego projektu (po drzewie, w kolejnosci dodawania)")
    @GetMapping("/api/projects/{projectId}/works")
    public ResponseEntity<List<TreeWorkResponse>> getTreeWorksOfProject(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                        @PathVariable Long projectId) {
        return ResponseEntity.ok(treeWorkFacade.getWorksOfProjectOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }
}

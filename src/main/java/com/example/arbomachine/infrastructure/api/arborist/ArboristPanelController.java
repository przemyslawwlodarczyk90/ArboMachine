package com.example.arbomachine.infrastructure.api.arborist;

import com.example.arbomachine.domain.user.dto.RolePanelWelcomeDto;
import com.example.arbomachine.domain.user.facade.RolePanelWelcomeFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Panel arborysty - tylko rola ARBORIST (wymuszone w SecurityConfig dla /api/arborist/**). */
@RestController
@RequestMapping("/api/arborist")
@Tag(name = "Panel arborysty", description = "Endpointy panelu arborysty (rola ARBORIST)")
public class ArboristPanelController {
    private static final String ARBORIST_PANEL_WELCOME_MESSAGE = "Witaj w panelu arborysty.";

    private final RolePanelWelcomeFacade rolePanelWelcomeFacade;

    public ArboristPanelController(RolePanelWelcomeFacade rolePanelWelcomeFacade) {
        this.rolePanelWelcomeFacade = rolePanelWelcomeFacade;
    }

    @Operation(summary = "Dane powitalne panelu arborysty")
    @GetMapping("/panel")
    public ResponseEntity<RolePanelWelcomeDto> getArboristPanelWelcome(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(rolePanelWelcomeFacade.buildPanelWelcomeForUser(authenticatedUser.getUsername(),
                ARBORIST_PANEL_WELCOME_MESSAGE));
    }
}

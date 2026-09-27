package com.example.arbomachine.infrastructure.api.official;

import com.example.arbomachine.domain.user.dto.RolePanelWelcomeDto;
import com.example.arbomachine.domain.user.service.RolePanelWelcomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Panel urzednika - tylko rola OFFICIAL (wymuszone w SecurityConfig dla /api/official/**). */
@RestController
@RequestMapping("/api/official")
@Tag(name = "Panel urzednika", description = "Endpointy panelu urzednika (rola OFFICIAL)")
public class OfficialPanelController {
    private static final String OFFICIAL_PANEL_WELCOME_MESSAGE = "Welcome to the official panel";

    private final RolePanelWelcomeService rolePanelWelcomeService;

    public OfficialPanelController(RolePanelWelcomeService rolePanelWelcomeService) {
        this.rolePanelWelcomeService = rolePanelWelcomeService;
    }

    @Operation(summary = "Dane powitalne panelu urzednika")
    @GetMapping("/panel")
    public ResponseEntity<RolePanelWelcomeDto> getOfficialPanelWelcome(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(rolePanelWelcomeService.buildPanelWelcomeForUser(authenticatedUser.getUsername(),
                OFFICIAL_PANEL_WELCOME_MESSAGE));
    }
}

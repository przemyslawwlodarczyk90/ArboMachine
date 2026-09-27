package com.example.arbomachine.infrastructure.api.admin;

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

/** Panel admina - tylko rola ADMIN (wymuszone w SecurityConfig dla /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Panel admina", description = "Endpointy panelu admina (rola ADMIN)")
public class AdminPanelController {
    private static final String ADMIN_PANEL_WELCOME_MESSAGE = "Welcome to the admin panel";

    private final RolePanelWelcomeFacade rolePanelWelcomeFacade;

    public AdminPanelController(RolePanelWelcomeFacade rolePanelWelcomeFacade) {
        this.rolePanelWelcomeFacade = rolePanelWelcomeFacade;
    }

    @Operation(summary = "Dane powitalne panelu admina")
    @GetMapping("/panel")
    public ResponseEntity<RolePanelWelcomeDto> getAdminPanelWelcome(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(rolePanelWelcomeFacade.buildPanelWelcomeForUser(authenticatedUser.getUsername(),
                ADMIN_PANEL_WELCOME_MESSAGE));
    }
}

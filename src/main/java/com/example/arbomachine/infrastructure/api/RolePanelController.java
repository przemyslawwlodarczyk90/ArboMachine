package com.example.arbomachine.infrastructure.api;

import com.example.arbomachine.domain.user.dto.RolePanelWelcomeDto;
import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.facade.UserAccountFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punkty wejscia paneli rol. Kazda sciezka jest ograniczona do jednej roli w SecurityConfig;
 * funkcje konkretnych paneli beda dochodzic pod tymi samymi prefiksami.
 */
@RestController
@Tag(name = "Panele rol", description = "Punkty wejscia paneli arborysty, urzednika i admina")
public class RolePanelController {

    private final UserAccountFacade userAccountFacade;

    public RolePanelController(UserAccountFacade userAccountFacade) {
        this.userAccountFacade = userAccountFacade;
    }

    @Operation(summary = "Panel arborysty (rola ARBORIST)")
    @GetMapping("/api/arborist/panel")
    public ResponseEntity<RolePanelWelcomeDto> getArboristPanelWelcome(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(buildPanelWelcome(authenticatedUser, "Welcome to the arborist panel"));
    }

    @Operation(summary = "Panel urzednika (rola OFFICIAL)")
    @GetMapping("/api/official/panel")
    public ResponseEntity<RolePanelWelcomeDto> getOfficialPanelWelcome(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(buildPanelWelcome(authenticatedUser, "Welcome to the official panel"));
    }

    @Operation(summary = "Panel admina (rola ADMIN)")
    @GetMapping("/api/admin/panel")
    public ResponseEntity<RolePanelWelcomeDto> getAdminPanelWelcome(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(buildPanelWelcome(authenticatedUser, "Welcome to the admin panel"));
    }

    private RolePanelWelcomeDto buildPanelWelcome(UserDetails authenticatedUser, String welcomeMessage) {
        UserAccountDto currentUserAccount = userAccountFacade.getCurrentUserAccount(authenticatedUser.getUsername());
        return new RolePanelWelcomeDto(currentUserAccount.role(), currentUserAccount.firstName(),
                currentUserAccount.lastName(), welcomeMessage);
    }
}

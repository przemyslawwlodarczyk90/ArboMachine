package com.example.arbomachine.infrastructure.api.documentauthor;

import com.example.arbomachine.domain.documentauthor.dto.DocumentAuthorProfileRequest;
import com.example.arbomachine.domain.documentauthor.dto.DocumentAuthorProfileResponse;
import com.example.arbomachine.domain.documentauthor.facade.DocumentAuthorProfileFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dane autora opracowania zalogowanego uzytkownika (SecurityConfig: /api/users/** - zalogowany). Uzywane na stronie
 * tytulowej i w opisie metod dokumentu projektu.
 */
@RestController
@Tag(name = "Autor opracowania", description = "Stale elementy dokumentu projektu: autor, kwalifikacje, wykonawca, opis metod")
public class DocumentAuthorProfileController {

    private final DocumentAuthorProfileFacade documentAuthorProfileFacade;

    public DocumentAuthorProfileController(DocumentAuthorProfileFacade documentAuthorProfileFacade) {
        this.documentAuthorProfileFacade = documentAuthorProfileFacade;
    }

    @Operation(summary = "Dane autora opracowania; brak danych -> 200 z hasProfile=false")
    @GetMapping("/api/users/me/document-author-profile")
    public ResponseEntity<DocumentAuthorProfileResponse> getProfile(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(documentAuthorProfileFacade.getProfileOfCurrentUser(authenticatedUser.getUsername()));
    }

    @Operation(summary = "Zapis danych autora opracowania (zastepuje calosc); wszystkie pola puste -> usuniecie danych")
    @PutMapping("/api/users/me/document-author-profile")
    public ResponseEntity<DocumentAuthorProfileResponse> saveProfile(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                     @Valid @RequestBody DocumentAuthorProfileRequest request) {
        return ResponseEntity.ok(documentAuthorProfileFacade.saveProfileOfCurrentUser(authenticatedUser.getUsername(), request));
    }
}

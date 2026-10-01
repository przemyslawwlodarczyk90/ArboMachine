package com.example.arbomachine.infrastructure.api.treephoto;

import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationDocumentRequest;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationResponse;
import com.example.arbomachine.domain.treephoto.dto.RenderedAnnotationImageUpload;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoContent;
import com.example.arbomachine.domain.treephoto.facade.TreePhotoAnnotationFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * Adnotacje zdjec i tresc zdjec do dokumentacji. Wymaga zalogowania (SecurityConfig: /api/projects/**);
 * cala hierarchie (projekt -> drzewo -> zdjecie -> adnotacja) sprawdza TreePhotoAnnotationFacade.
 * Klucze plikow nigdy nie wychodza do frontendu.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/trees/{treeId}/photos/{photoId}")
@Tag(name = "Adnotacje zdjec", description = "Szkice na zdjeciach drzew (oryginal zdjecia nigdy nie jest zmieniany)")
public class TreePhotoAnnotationController {

    private final TreePhotoAnnotationFacade treePhotoAnnotationFacade;

    public TreePhotoAnnotationController(TreePhotoAnnotationFacade treePhotoAnnotationFacade) {
        this.treePhotoAnnotationFacade = treePhotoAnnotationFacade;
    }

    @Operation(summary = "Aktualna adnotacja zdjecia (dokument wektorowy); brak adnotacji -> 404")
    @GetMapping("/annotation")
    public ResponseEntity<PhotoAnnotationResponse> getAnnotation(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                 @PathVariable Long projectId, @PathVariable Long treeId,
                                                                 @PathVariable Long photoId) {
        return ResponseEntity.ok(treePhotoAnnotationFacade.getAnnotationOfPhotoOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, photoId));
    }

    @Operation(summary = "Utworzenie albo zastapienie adnotacji: multipart z czesciami \"document\" (application/json, "
            + "schemaVersion 1) i \"renderedImage\" (image/png - zdjecie z naniesionym rysunkiem)")
    @PutMapping(value = "/annotation", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoAnnotationResponse> saveAnnotation(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                  @PathVariable Long projectId, @PathVariable Long treeId,
                                                                  @PathVariable Long photoId,
                                                                  @RequestPart("document") PhotoAnnotationDocumentRequest document,
                                                                  @RequestPart("renderedImage") MultipartFile renderedImage) {
        return ResponseEntity.ok(treePhotoAnnotationFacade.saveAnnotationOfPhotoOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId, photoId, document,
                new RenderedAnnotationImageUpload(renderedImage.getContentType(), renderedImage.getSize(), renderedImage)));
    }

    @Operation(summary = "Usuniecie adnotacji (rekord i splaszczony PNG); oryginal zdjecia zostaje")
    @DeleteMapping("/annotation")
    public ResponseEntity<Void> deleteAnnotation(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                 @PathVariable Long projectId, @PathVariable Long treeId,
                                                 @PathVariable Long photoId) {
        treePhotoAnnotationFacade.deleteAnnotationOfPhotoOfCurrentUser(authenticatedUser.getUsername(), projectId, treeId, photoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Splaszczony PNG adnotacji (zdjecie + rysunek)")
    @GetMapping("/annotation/content")
    public ResponseEntity<Resource> getAnnotationContent(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                         @PathVariable Long projectId, @PathVariable Long treeId,
                                                         @PathVariable Long photoId) {
        return contentResponse(treePhotoAnnotationFacade.getAnnotationContentOfPhotoOfCurrentUser(
                authenticatedUser.getUsername(), projectId, treeId, photoId));
    }

    @Operation(summary = "Zdjecie do dokumentacji: wersja z adnotacja, jesli istnieje, w przeciwnym razie oryginal")
    @GetMapping("/documentation-content")
    public ResponseEntity<Resource> getDocumentationContent(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                            @PathVariable Long projectId, @PathVariable Long treeId,
                                                            @PathVariable Long photoId) {
        return contentResponse(treePhotoAnnotationFacade.getDocumentationContentOfPhotoOfCurrentUser(
                authenticatedUser.getUsername(), projectId, treeId, photoId));
    }

    /** Tresc moze sie zmienic po edycji adnotacji - bez cache przegladarki (inaczej niz niezmienny oryginal). */
    private static ResponseEntity<Resource> contentResponse(TreePhotoContent content) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .contentLength(content.fileSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(content.originalFileName(), StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(content.content());
    }
}

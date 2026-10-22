package com.example.arbomachine.infrastructure.api.treephoto;

import com.example.arbomachine.domain.treephoto.dto.TreePhotoContent;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoResponse;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoUpload;
import com.example.arbomachine.domain.treephoto.facade.TreePhotoFacade;
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Zdjecia drzewa. Wymaga zalogowania (SecurityConfig: /api/projects/**). Id projektu, drzewa i zdjecia pochodza
 * wylacznie z URL; cala hierarchie sprawdza TreePhotoFacade (handlery). Frontend nigdy nie dostaje sciezki pliku -
 * tresc zdjecia jest serwowana przez endpoint .../content.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/trees/{treeId}/photos")
@Tag(name = "Zdjecia drzew", description = "Zdjecia drzew w projektach zalogowanego uzytkownika (JPEG, PNG, WebP)")
public class TreePhotoController {

    /** Tresc zdjecia pod danym id nigdy sie nie zmienia - przegladarka moze ja trzymac w prywatnym cache. */
    private static final Duration PHOTO_CONTENT_CACHE_DURATION = Duration.ofDays(1);

    private final TreePhotoFacade treePhotoFacade;

    public TreePhotoController(TreePhotoFacade treePhotoFacade) {
        this.treePhotoFacade = treePhotoFacade;
    }

    @Operation(summary = "Dodanie zdjecia (multipart, pole \"file\"; JPEG/PNG/WebP, limit app.tree-photos.max-file-size); "
            + "typ sprawdzany z zawartosci pliku; cudzy projekt/drzewo daje 404. Opcjonalnie \"clientUploadId\" (UUID z "
            + "urzadzenia, etap 33): ponowne wyslanie z tym samym id zwraca juz zapisane zdjecie zamiast duplikatu")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TreePhotoResponse> uploadTreePhoto(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                             @PathVariable Long projectId,
                                                             @PathVariable Long treeId,
                                                             @RequestParam("file") MultipartFile file,
                                                             @RequestParam(value = "clientUploadId", required = false)
                                                             String clientUploadId) {
        TreePhotoResponse uploadedTreePhoto = treePhotoFacade.uploadPhotoOfTreeOfCurrentUser(
                authenticatedUser.getUsername(), projectId, treeId,
                new TreePhotoUpload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file, clientUploadId));
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/trees/" + treeId + "/photos/"
                + uploadedTreePhoto.id() + "/content")).body(uploadedTreePhoto);
    }

    @Operation(summary = "Metadane zdjec drzewa w kolejnosci dodawania (bez sciezek plikow)")
    @GetMapping
    public ResponseEntity<List<TreePhotoResponse>> getTreePhotos(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                 @PathVariable Long projectId,
                                                                 @PathVariable Long treeId) {
        return ResponseEntity.ok(treePhotoFacade.getPhotosOfTreeOfCurrentUser(authenticatedUser.getUsername(),
                projectId, treeId));
    }

    @Operation(summary = "Tresc zdjecia; zdjecie innego drzewa albo cudzy projekt daje 404")
    @GetMapping("/{photoId}/content")
    public ResponseEntity<Resource> getTreePhotoContent(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                        @PathVariable Long projectId,
                                                        @PathVariable Long treeId,
                                                        @PathVariable Long photoId) {
        TreePhotoContent treePhotoContent = treePhotoFacade.getPhotoContentOfTreeOfCurrentUser(
                authenticatedUser.getUsername(), projectId, treeId, photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(treePhotoContent.contentType()))
                .contentLength(treePhotoContent.fileSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(treePhotoContent.originalFileName(), StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.maxAge(PHOTO_CONTENT_CACHE_DURATION).cachePrivate())
                .body(treePhotoContent.content());
    }

    @Operation(summary = "Usuniecie zdjecia (rekord i plik); zdjecie innego drzewa albo cudzy projekt daje 404")
    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> deleteTreePhoto(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                @PathVariable Long projectId,
                                                @PathVariable Long treeId,
                                                @PathVariable Long photoId) {
        treePhotoFacade.deletePhotoOfTreeOfCurrentUser(authenticatedUser.getUsername(), projectId, treeId, photoId);
        return ResponseEntity.noContent().build();
    }
}

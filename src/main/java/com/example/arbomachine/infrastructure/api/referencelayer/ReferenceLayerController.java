package com.example.arbomachine.infrastructure.api.referencelayer;

import com.example.arbomachine.domain.referencelayer.dto.ReferenceLayerResponse;
import com.example.arbomachine.domain.referencelayer.dto.ReferenceLayerUpload;
import com.example.arbomachine.domain.referencelayer.facade.ReferenceLayerFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/** Warstwy referencyjne projektu (etap 31): pliki GIS/CAD z QGIS pokazywane pod drzewami na mapie. Wlasnosc: projekt. */
@RestController
@RequestMapping("/api/projects/{projectId}/reference-layers")
@Tag(name = "Warstwy referencyjne", description = "Podklad wektorowy projektu z plikow GeoJSON / Shapefile ZIP / GeoPackage / DXF")
public class ReferenceLayerController {

    private final ReferenceLayerFacade referenceLayerFacade;

    public ReferenceLayerController(ReferenceLayerFacade referenceLayerFacade) {
        this.referenceLayerFacade = referenceLayerFacade;
    }

    @Operation(summary = "Warstwy referencyjne projektu (bez geometrii)")
    @GetMapping
    public ResponseEntity<List<ReferenceLayerResponse>> getLayers(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                  @PathVariable Long projectId) {
        return ResponseEntity.ok(referenceLayerFacade.getLayersOfProjectOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Nowa warstwa z pliku (pole \"file\": .geojson/.json, Shapefile .zip, .gpkg, .dxf; opcjonalnie \"name\")")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReferenceLayerResponse> createLayer(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                              @PathVariable Long projectId, @RequestPart("file") MultipartFile file,
                                                              @RequestParam(value = "name", required = false) String name) throws IOException {
        ReferenceLayerResponse created = referenceLayerFacade.createLayerOfCurrentUser(authenticatedUser.getUsername(), projectId,
                new ReferenceLayerUpload(file.getOriginalFilename(), file.getBytes(), name));
        return ResponseEntity.created(URI.create("/api/projects/" + projectId + "/reference-layers/" + created.id())).body(created);
    }

    @Operation(summary = "Geometria warstwy - GeoJSON FeatureCollection w WGS 84")
    @GetMapping(value = "/{layerId}/geojson", produces = "application/geo+json")
    public ResponseEntity<String> getLayerGeoJson(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                  @PathVariable Long projectId, @PathVariable Long layerId) {
        return ResponseEntity.ok(referenceLayerFacade.getLayerGeoJsonOfCurrentUser(authenticatedUser.getUsername(), projectId, layerId));
    }

    @Operation(summary = "Usuniecie warstwy")
    @DeleteMapping("/{layerId}")
    public ResponseEntity<Void> deleteLayer(@AuthenticationPrincipal UserDetails authenticatedUser,
                                            @PathVariable Long projectId, @PathVariable Long layerId) {
        referenceLayerFacade.deleteLayerOfCurrentUser(authenticatedUser.getUsername(), projectId, layerId);
        return ResponseEntity.noContent().build();
    }
}

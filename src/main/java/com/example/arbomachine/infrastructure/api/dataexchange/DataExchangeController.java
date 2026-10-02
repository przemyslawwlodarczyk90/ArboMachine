package com.example.arbomachine.infrastructure.api.dataexchange;

import com.example.arbomachine.domain.dataexchange.dto.TreeImportResultResponse;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportUpload;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportValidationResponse;
import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.facade.DataExchangeFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Eksport drzew projektu (CSV, XLSX, GeoJSON, GeoPackage) i import drzew (CSV, XLSX, GeoJSON: najpierw "validate", potem import).
 * Wymaga zalogowania (SecurityConfig: /api/projects/**); wlasnosc projektu sprawdzaja handlery - cudzy -> 404.
 * Identyfikatory projektu i wlasciciela pochodza wylacznie z URL i JWT, nigdy z pliku.
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
@Tag(name = "Import i eksport", description = "Wymiana danych drzew: CSV, XLSX, GeoJSON (WGS 84 / EPSG:4326)")
public class DataExchangeController {

    private final DataExchangeFacade dataExchangeFacade;

    public DataExchangeController(DataExchangeFacade dataExchangeFacade) {
        this.dataExchangeFacade = dataExchangeFacade;
    }

    @Operation(summary = "Drzewa projektu jako CSV (separator \";\", UTF-8 z BOM, przecinek dziesietny, naglowki po polsku)")
    @GetMapping("/exports/trees.csv")
    public ResponseEntity<byte[]> exportTreesAsCsv(@AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId) {
        return fileResponse(dataExchangeFacade.exportTreesAsCsvOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Prace projektu jako CSV (etap 28: jeden wiersz = jedna praca, status/priorytet jako kody)")
    @GetMapping("/exports/works.csv")
    public ResponseEntity<byte[]> exportTreeWorksAsCsv(@AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId) {
        return fileResponse(dataExchangeFacade.exportTreeWorksAsCsvOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Grupy drzew projektu jako GeoJSON (Polygon, WGS 84) - etap 29")
    @GetMapping("/exports/stands.geojson")
    public ResponseEntity<byte[]> exportTreeStandsAsGeoJson(@AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId) {
        return fileResponse(dataExchangeFacade.exportTreeStandsAsGeoJsonOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Shapefile ZIP: warstwy trees (Point) i stands (Polygon), WGS 84, UTF-8 - etap 29")
    @GetMapping("/exports/shapefile.zip")
    public ResponseEntity<byte[]> exportProjectAsShapefile(@AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId) {
        return fileResponse(dataExchangeFacade.exportProjectAsShapefileOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Kalendarz prac projektu (.ics, RFC 5545): prace do wykonania z terminem jako wydarzenia calodniowe (etap 30)")
    @GetMapping("/exports/calendar.ics")
    public ResponseEntity<byte[]> exportTreeWorksAsIcs(@AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId) {
        return fileResponse(dataExchangeFacade.exportTreeWorksAsIcsOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Drzewa projektu jako XLSX (arkusze \"Drzewa\", \"Prace\", \"Monitoring\" i \"Projekt\")")
    @GetMapping("/exports/trees.xlsx")
    public ResponseEntity<byte[]> exportTreesAsXlsx(@AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId) {
        return fileResponse(dataExchangeFacade.exportTreesAsXlsxOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Drzewa z lokalizacja jako GeoJSON FeatureCollection (Point, WGS 84) - schemat ArboMachine V1")
    @GetMapping("/exports/trees.geojson")
    public ResponseEntity<byte[]> exportTreesAsGeoJson(@AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId) {
        return fileResponse(dataExchangeFacade.exportTreesAsGeoJsonOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Drzewa z lokalizacja jako GeoPackage (warstwa \"trees\", POINT, EPSG:4326) - QGIS compatibility profile V1")
    @GetMapping("/exports/trees.gpkg")
    public ResponseEntity<byte[]> exportTreesAsGeoPackage(@AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId) {
        return fileResponse(dataExchangeFacade.exportTreesAsGeoPackageOfCurrentUser(authenticatedUser.getUsername(), projectId));
    }

    @Operation(summary = "Sprawdzenie pliku importu drzew (CSV/XLSX/GeoJSON/Shapefile ZIP/GeoPackage/DXF, pole \"file\") - nic nie zapisuje")
    @PostMapping(value = "/imports/trees/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TreeImportValidationResponse> validateTreeImport(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                           @PathVariable Long projectId,
                                                                           @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(dataExchangeFacade.validateTreeImportOfCurrentUser(authenticatedUser.getUsername(), projectId,
                new TreeImportUpload(file.getOriginalFilename(), file.getBytes())));
    }

    @Operation(summary = "Import drzew - wszystko albo nic: bledy -> 400 z wynikiem walidacji; kazdy wiersz = nowe drzewo")
    @PostMapping(value = "/imports/trees", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TreeImportResultResponse> importTrees(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                @PathVariable Long projectId,
                                                                @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(dataExchangeFacade.importTreesOfCurrentUser(
                authenticatedUser.getUsername(), projectId, new TreeImportUpload(file.getOriginalFilename(), file.getBytes())));
    }

    @Operation(summary = "Sprawdzenie pliku obszarow grup drzew (DXF, Shapefile ZIP, GeoJSON, GeoPackage; pole \"file\") - nic nie zapisuje")
    @PostMapping(value = "/imports/stands/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.example.arbomachine.domain.dataexchange.dto.TreeStandImportValidationResponse> validateTreeStandImport(
            @AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(dataExchangeFacade.validateTreeStandImportOfCurrentUser(authenticatedUser.getUsername(), projectId,
                new TreeImportUpload(file.getOriginalFilename(), file.getBytes())));
    }

    @Operation(summary = "Import grup drzew - wszystko albo nic; kazdy obszar = nowa grupa, drzewa w obszarze przypisane automatycznie")
    @PostMapping(value = "/imports/stands", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.example.arbomachine.domain.dataexchange.dto.TreeStandImportResultResponse> importTreeStands(
            @AuthenticationPrincipal UserDetails authenticatedUser, @PathVariable Long projectId,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(dataExchangeFacade.importTreeStandsOfCurrentUser(
                authenticatedUser.getUsername(), projectId, new TreeImportUpload(file.getOriginalFilename(), file.getBytes())));
    }

    private static ResponseEntity<byte[]> fileResponse(ExportedFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(file.content());
    }
}

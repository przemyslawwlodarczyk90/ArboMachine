package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.crs.PolishGridCrs;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportUpload;
import com.example.arbomachine.domain.dataexchange.dto.TreeStandImportValidationResponse;
import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader;
import com.example.arbomachine.domain.dataexchange.gis.DxfLayerExtractor;
import com.example.arbomachine.domain.dataexchange.gis.GeoJsonGisReader;
import com.example.arbomachine.domain.dataexchange.gis.GeoPackageFileReader;
import com.example.arbomachine.domain.dataexchange.gis.GisFeature;
import com.example.arbomachine.domain.dataexchange.gis.GisFileException;
import com.example.arbomachine.domain.dataexchange.gis.GisLayer;
import com.example.arbomachine.domain.dataexchange.gis.ShapefileZipReader;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treestand.entity.TreeStand;
import com.example.arbomachine.domain.treestand.geometry.InvalidStandGeometryException;
import com.example.arbomachine.domain.treestand.geometry.StandPolygon;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Analiza pliku obszarow grup drzew (etap 29) dla WLASNEGO projektu: DXF (warstwy "-Area"), Shapefile ZIP, GeoPackage
 * i GeoJSON - obiekty Polygon / MultiPolygon (kazdy wielokat MultiPolygon = osobna grupa). Uklad PL-2000 / PL-1992
 * przeliczany do WGS 84. Nazwa z atrybutu name / nazwa (inaczej "Grupa drzew N"), uwagi z notes / uwagi. Nic nie zapisuje.
 */
@Component
public class TreeStandImportAnalyzer {

    public static final int MAXIMUM_STAND_COUNT = 500;
    private static final int PREVIEW_COUNT = 10;
    private static final List<String> NAME_ATTRIBUTES = List.of("name", "nazwa", "stand_name", "standname");
    private static final List<String> NOTES_ATTRIBUTES = List.of("notes", "uwagi", "opis", "description");

    private final TreeRepository treeRepository;
    private final ObjectMapper objectMapper;

    public TreeStandImportAnalyzer(TreeRepository treeRepository, ObjectMapper objectMapper) {
        this.treeRepository = treeRepository;
        this.objectMapper = objectMapper;
    }

    /** Obszar gotowy do zapisu: nazwa, uwagi, wielokat WGS 84 i drzewa projektu lezace w nim. */
    public record ValidStand(int rowNumber, String name, String notes, StandPolygon polygon, Set<Tree> treesInside) {}

    public record TreeStandImportAnalysis(TreeStandImportValidationResponse response, List<ValidStand> validStands) {}

    public TreeStandImportAnalysis analyze(Project ownedProject, TreeImportUpload upload) {
        String fileName = upload.originalFileName() == null ? "" : upload.originalFileName();
        Optional<TreeImportFormat> format = TreeImportFormat.fromFileName(fileName)
                .filter(candidate -> candidate != TreeImportFormat.CSV && candidate != TreeImportFormat.XLSX);
        if (format.isEmpty()) return rejected(fileName, null, TreeImportIssue.ofFile(TreeImportIssueCode.UNSUPPORTED_FILE_FORMAT, null));
        if (upload.content() == null || upload.content().length == 0) {
            return rejected(fileName, format.get(), TreeImportIssue.ofFile(TreeImportIssueCode.EMPTY_FILE, null));
        }
        if (upload.content().length > TreeImportAnalyzer.MAXIMUM_FILE_SIZE_BYTES) {
            return rejected(fileName, format.get(), TreeImportIssue.ofFile(TreeImportIssueCode.FILE_TOO_LARGE, null));
        }
        List<GisLayer> polygonLayers;
        try {
            polygonLayers = layersOf(format.get(), upload.content()).stream().filter(GisLayer::hasPolygons).toList();
        } catch (GisFileException gisFileException) {
            return rejected(fileName, format.get(), GisTreeRows.fileExceptionOf(gisFileException).toIssue());
        }
        if (polygonLayers.isEmpty()) return rejected(fileName, format.get(), TreeImportIssue.ofFile(TreeImportIssueCode.NO_POLYGON_FEATURES, null));

        List<Tree> projectTrees = treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(ownedProject.getId());
        List<ValidStand> validStands = new ArrayList<>();
        List<TreeImportIssue> errors = new ArrayList<>();
        List<TreeImportIssue> warnings = new ArrayList<>();
        int featureCount = 0;
        for (GisLayer layer : polygonLayers) {
            PolishGridCrs crs;
            try {
                crs = GisTreeRows.crsOf(layer);
            } catch (TreeImportFileException unsupportedCrs) {
                return rejected(fileName, format.get(), unsupportedCrs.toIssue());
            }
            if (crs != PolishGridCrs.WGS84) warnings.add(TreeImportIssue.ofFile(TreeImportIssueCode.COORDINATES_CONVERTED, crs.code()));
            for (GisFeature feature : layer.features()) {
                List<GisFeature.Polygon> polygons = switch (feature.geometry()) {
                    case GisFeature.Polygon polygon -> List.of(polygon);
                    case GisFeature.MultiPolygon multiPolygon -> multiPolygon.polygons();
                    case null, default -> List.of();
                };
                if (polygons.isEmpty()) continue;
                for (int part = 0; part < polygons.size(); part++) {
                    int rowNumber = ++featureCount;
                    try {
                        StandPolygon polygon = StandPolygon.ofRings(toWgs84(polygons.get(part), crs));
                        String baseName = attribute(feature.attributes(), NAME_ATTRIBUTES).orElse("Grupa drzew " + rowNumber);
                        String name = polygons.size() > 1 ? baseName + " (" + (part + 1) + ")" : baseName;
                        validStands.add(new ValidStand(rowNumber, truncate(name, TreeStand.MAXIMUM_NAME_LENGTH),
                                attribute(feature.attributes(), NOTES_ATTRIBUTES).map(notes -> truncate(notes, TreeStand.MAXIMUM_NOTES_LENGTH)).orElse(null),
                                polygon, treesInside(projectTrees, polygon)));
                    } catch (InvalidStandGeometryException invalidPolygon) {
                        errors.add(new TreeImportIssue(rowNumber, "geometry", "geometria", invalidPolygon.getMessage(), TreeImportIssueCode.INVALID_POLYGON));
                    }
                }
            }
        }
        if (featureCount > MAXIMUM_STAND_COUNT) return rejected(fileName, format.get(), TreeImportIssue.ofFile(TreeImportIssueCode.TOO_MANY_ROWS, null));
        List<TreeStandImportValidationResponse.PreviewStand> preview = validStands.stream().limit(PREVIEW_COUNT)
                .map(stand -> new TreeStandImportValidationResponse.PreviewStand(stand.rowNumber(), stand.name(),
                        stand.polygon().roundedAreaSquareMeters(), stand.polygon().vertexCount(), stand.treesInside().size()))
                .toList();
        TreeStandImportValidationResponse response = new TreeStandImportValidationResponse(fileName, format.get().name(), featureCount,
                validStands.size(), featureCount - validStands.size(), errors.isEmpty() && !validStands.isEmpty(), List.of(), errors,
                warnings.stream().distinct().toList(), preview);
        return new TreeStandImportAnalysis(response, validStands);
    }

    private List<GisLayer> layersOf(TreeImportFormat format, byte[] content) {
        return switch (format) {
            case SHAPEFILE -> ShapefileZipReader.read(content);
            case GEOPACKAGE -> GeoPackageFileReader.read(content);
            case DXF -> List.of(DxfLayerExtractor.standLayer(DxfDrawingReader.read(content)));
            case GEOJSON -> List.of(GeoJsonGisReader.read(content, objectMapper));
            case CSV, XLSX -> List.of();
        };
    }

    private static List<List<double[]>> toWgs84(GisFeature.Polygon polygon, PolishGridCrs crs) {
        return polygon.rings().stream().map(ring -> ring.stream().map(position -> crs.toWgs84(position[0], position[1])).toList()).toList();
    }

    private static Set<Tree> treesInside(List<Tree> projectTrees, StandPolygon polygon) {
        return projectTrees.stream().filter(tree -> tree.getLocation() != null && tree.getLocation().getLatitude() != null
                        && polygon.contains(tree.getLocation().getLongitude().doubleValue(), tree.getLocation().getLatitude().doubleValue()))
                .collect(Collectors.toSet());
    }

    private static Optional<String> attribute(Map<String, String> attributes, List<String> names) {
        return attributes.entrySet().stream()
                .filter(entry -> names.contains(entry.getKey().toLowerCase(Locale.ROOT)) && entry.getValue() != null && !entry.getValue().isBlank())
                .map(entry -> entry.getValue().trim()).findFirst();
    }

    private static String truncate(String text, int maximumLength) {
        return text.length() <= maximumLength ? text : text.substring(0, maximumLength);
    }

    private static TreeStandImportAnalysis rejected(String fileName, TreeImportFormat format, TreeImportIssue fileError) {
        return new TreeStandImportAnalysis(new TreeStandImportValidationResponse(fileName, format == null ? null : format.name(), 0, 0, 0,
                false, List.of(fileError), List.of(), List.of(), List.of()), List.of());
    }
}

package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GeoJSON drzew (np. eksport ArboMachine po edycji w QGIS): FeatureCollection albo pojedynczy Feature.
 * Wspolrzedne tylko WGS 84 - czlon "crs" (stary GeoJSON 2008, QGIS) dopuszczalny wylacznie dla CRS84 / EPSG:4326.
 * Geometria Point [dlugosc, szerokosc] -> lokalizacja; brak geometrii -> drzewo bez lokalizacji; inna geometria ->
 * blad wiersza. Wlasciwosci dopasowywane jak naglowki CSV (nazwa techniczna, GIS albo polska). Pola tylko eksportowe
 * (treeId, mapSymbolCode, works...) sa IGNOROWANE - import zawsze tworzy nowe drzewa, nigdy nie aktualizuje po id.
 */
@Component
public class GeoJsonTreeImportParser implements TreeImportParser {

    private static final Set<String> ACCEPTED_CRS_NAMES = Set.of("urn:ogc:def:crs:OGC:1.3:CRS84", "urn:ogc:def:crs:OGC::CRS84",
            "EPSG:4326", "urn:ogc:def:crs:EPSG::4326");

    private final ObjectMapper objectMapper;

    public GeoJsonTreeImportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public TreeImportFormat format() {
        return TreeImportFormat.GEOJSON;
    }

    @Override
    public ParsedTreeImportFile parse(byte[] content) {
        JsonNode root;
        try {
            root = objectMapper.readTree(content);
        } catch (IOException invalidJson) {
            throw new TreeImportFileException(TreeImportIssueCode.INVALID_GEOJSON);
        }
        if (root == null || root.isMissingNode() || root.isNull()) throw new TreeImportFileException(TreeImportIssueCode.EMPTY_FILE);
        if (!root.isObject()) throw new TreeImportFileException(TreeImportIssueCode.INVALID_GEOJSON);
        validateCrs(root.get("crs"));
        List<JsonNode> features = switch (root.path("type").asText()) {
            case "FeatureCollection" -> {
                if (!root.path("features").isArray()) throw new TreeImportFileException(TreeImportIssueCode.INVALID_GEOJSON);
                List<JsonNode> collected = new ArrayList<>();
                root.path("features").forEach(collected::add);
                yield collected;
            }
            case "Feature" -> List.of(root);
            default -> throw new TreeImportFileException(TreeImportIssueCode.INVALID_GEOJSON);
        };
        if (features.isEmpty()) throw new TreeImportFileException(TreeImportIssueCode.NO_DATA_ROWS);

        Set<TreeExchangeField> recognizedFields = new LinkedHashSet<>();
        Set<String> ignoredProperties = new LinkedHashSet<>();
        List<ImportedTreeRow> rows = new ArrayList<>();
        for (int featureIndex = 0; featureIndex < features.size(); featureIndex++) {
            rows.add(rowOf(featureIndex + 1, features.get(featureIndex), recognizedFields, ignoredProperties));
        }
        if (!recognizedFields.contains(TreeExchangeField.INVENTORY_NUMBER)) {
            throw new TreeImportFileException(TreeImportIssueCode.MISSING_REQUIRED_COLUMN,
                    TreeExchangeField.INVENTORY_NUMBER.technicalName());
        }
        return new ParsedTreeImportFile(recognizedFields, List.copyOf(ignoredProperties), rows);
    }

    private static void validateCrs(JsonNode crs) {
        if (crs == null || crs.isNull()) return;
        String crsName = crs.path("properties").path("name").asText("");
        if (!ACCEPTED_CRS_NAMES.contains(crsName)) throw new TreeImportFileException(TreeImportIssueCode.UNSUPPORTED_CRS, crsName);
    }

    private static ImportedTreeRow rowOf(int featureNumber, JsonNode feature, Set<TreeExchangeField> recognizedFields,
                                         Set<String> ignoredProperties) {
        if (!feature.isObject() || !"Feature".equals(feature.path("type").asText())) {
            throw new TreeImportFileException(TreeImportIssueCode.INVALID_GEOJSON);
        }
        Map<TreeExchangeField, ImportedCell> cells = new EnumMap<>(TreeExchangeField.class);
        List<TreeImportIssue> readingIssues = new ArrayList<>();
        JsonNode properties = feature.path("properties");
        if (properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> propertyIterator = properties.fields();
            while (propertyIterator.hasNext()) {
                Map.Entry<String, JsonNode> property = propertyIterator.next();
                var field = TreeExchangeField.fromHeader(property.getKey());
                if (field.isEmpty() || !field.get().importable()
                        || field.get() == TreeExchangeField.LATITUDE || field.get() == TreeExchangeField.LONGITUDE) {
                    ignoredProperties.add(property.getKey());
                    continue;
                }
                recognizedFields.add(field.get());
                cells.put(field.get(), new ImportedCell(field.get(), property.getKey(), textOf(property.getValue())));
            }
        }
        JsonNode geometry = feature.get("geometry");
        if (geometry != null && !geometry.isNull()) {
            JsonNode coordinates = geometry.path("coordinates");
            if (!"Point".equals(geometry.path("type").asText()) || !coordinates.isArray() || coordinates.size() < 2) {
                readingIssues.add(new TreeImportIssue(featureNumber, "geometry", "geometry", geometry.path("type").asText(),
                        TreeImportIssueCode.UNSUPPORTED_GEOMETRY));
            } else {
                // RFC 7946: [dlugosc, szerokosc] - nigdy odwrotnie.
                cells.put(TreeExchangeField.LONGITUDE, new ImportedCell(TreeExchangeField.LONGITUDE, "geometry", textOf(coordinates.get(0))));
                cells.put(TreeExchangeField.LATITUDE, new ImportedCell(TreeExchangeField.LATITUDE, "geometry", textOf(coordinates.get(1))));
            }
        }
        return new ImportedTreeRow(featureNumber, cells, readingIssues);
    }

    private static String textOf(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (value.isNumber()) return value.decimalValue().stripTrailingZeros().toPlainString();
        if (value.isValueNode()) return value.asText();
        return value.toString();
    }
}

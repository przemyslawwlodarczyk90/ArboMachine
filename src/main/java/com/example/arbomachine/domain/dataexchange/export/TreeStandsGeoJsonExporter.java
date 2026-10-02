package com.example.arbomachine.domain.dataexchange.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Grupy drzew jako GeoJSON (etap 29, RFC 7946, WGS 84): Feature z geometria Polygon i atrybutami standId, name, notes,
 * areaSquareMeters, treeCount, treeInventoryNumbers. Do QGIS obok trees.geojson (albo jedna paczka GeoPackage / SHP).
 */
@Component
public class TreeStandsGeoJsonExporter {
    private final ObjectMapper objectMapper;

    public TreeStandsGeoJsonExporter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ExportedFile export(ProjectExportData data) {
        ObjectNode collection = objectMapper.createObjectNode();
        collection.put("type", "FeatureCollection");
        collection.put("name", "stands");
        ArrayNode features = collection.putArray("features");
        for (TreeStandExportData stand : data.stands()) {
            ObjectNode feature = features.addObject();
            feature.put("type", "Feature");
            ObjectNode geometry = feature.putObject("geometry");
            geometry.put("type", "Polygon");
            ArrayNode coordinates = geometry.putArray("coordinates");
            for (List<double[]> ring : stand.rings()) {
                ArrayNode ringNode = coordinates.addArray();
                for (double[] position : ring) ringNode.addArray().add(position[0]).add(position[1]);
            }
            ObjectNode properties = feature.putObject("properties");
            properties.put("standId", stand.standId());
            properties.put("name", stand.name());
            properties.put("notes", stand.notes());
            properties.put("areaSquareMeters", stand.areaSquareMeters());
            properties.put("treeCount", stand.treeInventoryNumbers().size());
            properties.put("treeInventoryNumbers", stand.treeNumbersText());
            properties.put("source", stand.source());
        }
        try {
            return new ExportedFile(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(collection),
                    "application/geo+json", "arbomachine-projekt-" + data.projectId() + "-grupy-drzew.geojson");
        } catch (JsonProcessingException serializationError) {
            throw new IllegalStateException(serializationError);
        }
    }
}

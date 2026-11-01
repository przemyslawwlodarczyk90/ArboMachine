package com.example.arbomachine.domain.treestand.service.treestandfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treestand.dto.TreeStandResponse;
import com.example.arbomachine.domain.treestand.entity.TreeStand;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Encja -> odpowiedz API (geometria jako obiekt JSON, id drzew rosnaco). */
@Component
public class TreeStandMapper {
    private final ObjectMapper objectMapper;

    public TreeStandMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TreeStandResponse toResponse(TreeStand stand) {
        try {
            return new TreeStandResponse(stand.getId(), stand.getProject().getId(), stand.getName(), stand.getNotes(),
                    objectMapper.readTree(stand.getGeometryGeoJson()), stand.getAreaSquareMeters(), stand.getSource().name(),
                    stand.getTrees().stream().map(Tree::getId).sorted().toList(), stand.getCreatedAt(), stand.getUpdatedAt());
        } catch (JacksonException corruptedGeometry) {
            throw new IllegalStateException("Stored stand geometry is not valid JSON", corruptedGeometry);
        }
    }
}

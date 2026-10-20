package com.example.arbomachine.domain.referencelayer.service.referencelayerfacade;

import com.example.arbomachine.domain.referencelayer.dto.ReferenceLayerResponse;
import com.example.arbomachine.domain.referencelayer.entity.ProjectReferenceLayer;
import org.springframework.stereotype.Component;

/** Encja -> odpowiedz API (bez geometrii). */
@Component
public class ReferenceLayerMapper {

    public ReferenceLayerResponse toResponse(ProjectReferenceLayer layer) {
        return new ReferenceLayerResponse(layer.getId(), layer.getProject().getId(), layer.getName(), layer.getSourceFileName(),
                layer.getSourceFormat(), layer.getSourceCrs(), layer.getFeatureCount(), layer.getCreatedAt());
    }
}

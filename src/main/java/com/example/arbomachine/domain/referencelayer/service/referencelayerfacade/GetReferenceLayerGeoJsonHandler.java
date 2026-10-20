package com.example.arbomachine.domain.referencelayer.service.referencelayerfacade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Geometria warstwy (GeoJSON FeatureCollection, WGS 84) jako gotowy tekst - bez ponownego parsowania. */
@Component
public class GetReferenceLayerGeoJsonHandler {
    private final FindReferenceLayerOfProjectOwnedByCurrentUserHandler findReferenceLayerOfProjectOwnedByCurrentUserHandler;

    public GetReferenceLayerGeoJsonHandler(FindReferenceLayerOfProjectOwnedByCurrentUserHandler findReferenceLayerOfProjectOwnedByCurrentUserHandler) {
        this.findReferenceLayerOfProjectOwnedByCurrentUserHandler = findReferenceLayerOfProjectOwnedByCurrentUserHandler;
    }

    @Transactional(readOnly = true)
    public String handle(String currentUserEmail, Long projectId, Long layerId) {
        return findReferenceLayerOfProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, layerId).getGeoJson();
    }
}

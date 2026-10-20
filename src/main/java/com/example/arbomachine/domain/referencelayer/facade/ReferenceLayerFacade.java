package com.example.arbomachine.domain.referencelayer.facade;

import com.example.arbomachine.domain.referencelayer.dto.ReferenceLayerResponse;
import com.example.arbomachine.domain.referencelayer.dto.ReferenceLayerUpload;
import com.example.arbomachine.domain.referencelayer.service.referencelayerfacade.CreateReferenceLayerHandler;
import com.example.arbomachine.domain.referencelayer.service.referencelayerfacade.DeleteReferenceLayerHandler;
import com.example.arbomachine.domain.referencelayer.service.referencelayerfacade.GetReferenceLayerGeoJsonHandler;
import com.example.arbomachine.domain.referencelayer.service.referencelayerfacade.GetReferenceLayersOfProjectHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/** Warstwy referencyjne projektu (etap 31, podklad wektorowy z QGIS) - tylko deleguje do handlerow. */
@Component
public class ReferenceLayerFacade {
    private final GetReferenceLayersOfProjectHandler getReferenceLayersOfProjectHandler;
    private final GetReferenceLayerGeoJsonHandler getReferenceLayerGeoJsonHandler;
    private final CreateReferenceLayerHandler createReferenceLayerHandler;
    private final DeleteReferenceLayerHandler deleteReferenceLayerHandler;

    public ReferenceLayerFacade(GetReferenceLayersOfProjectHandler getReferenceLayersOfProjectHandler,
                                GetReferenceLayerGeoJsonHandler getReferenceLayerGeoJsonHandler,
                                CreateReferenceLayerHandler createReferenceLayerHandler, DeleteReferenceLayerHandler deleteReferenceLayerHandler) {
        this.getReferenceLayersOfProjectHandler = getReferenceLayersOfProjectHandler;
        this.getReferenceLayerGeoJsonHandler = getReferenceLayerGeoJsonHandler;
        this.createReferenceLayerHandler = createReferenceLayerHandler;
        this.deleteReferenceLayerHandler = deleteReferenceLayerHandler;
    }

    public List<ReferenceLayerResponse> getLayersOfProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        return getReferenceLayersOfProjectHandler.handle(currentUserEmail, projectId);
    }

    public String getLayerGeoJsonOfCurrentUser(String currentUserEmail, Long projectId, Long layerId) {
        return getReferenceLayerGeoJsonHandler.handle(currentUserEmail, projectId, layerId);
    }

    public ReferenceLayerResponse createLayerOfCurrentUser(String currentUserEmail, Long projectId, ReferenceLayerUpload upload) {
        return createReferenceLayerHandler.handle(currentUserEmail, projectId, upload);
    }

    public void deleteLayerOfCurrentUser(String currentUserEmail, Long projectId, Long layerId) {
        deleteReferenceLayerHandler.handle(currentUserEmail, projectId, layerId);
    }
}

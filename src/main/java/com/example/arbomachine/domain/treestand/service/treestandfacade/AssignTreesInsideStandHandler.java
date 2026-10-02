package com.example.arbomachine.domain.treestand.service.treestandfacade;

import com.example.arbomachine.domain.treestand.dto.TreeStandResponse;
import com.example.arbomachine.domain.treestand.entity.TreeStand;
import com.example.arbomachine.domain.treestand.geometry.StandPolygon;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** "Przypisz drzewa w obszarze": dopisuje drzewa projektu, ktorych punkt lezy w wielokacie (nic nie usuwa). */
@Component
public class AssignTreesInsideStandHandler {
    private final FindTreeStandOfProjectOwnedByCurrentUserHandler findTreeStandHandler;
    private final TreesOfStandResolver treesOfStandResolver;
    private final TreeStandMapper treeStandMapper;
    private final ObjectMapper objectMapper;

    public AssignTreesInsideStandHandler(FindTreeStandOfProjectOwnedByCurrentUserHandler findTreeStandHandler,
                                         TreesOfStandResolver treesOfStandResolver, TreeStandMapper treeStandMapper,
                                         ObjectMapper objectMapper) {
        this.findTreeStandHandler = findTreeStandHandler;
        this.treesOfStandResolver = treesOfStandResolver;
        this.treeStandMapper = treeStandMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TreeStandResponse handle(String currentUserEmail, Long projectId, Long standId) {
        TreeStand stand = findTreeStandHandler.handle(currentUserEmail, projectId, standId);
        StandPolygon polygon = StandPolygon.fromGeoJsonText(stand.getGeometryGeoJson(), objectMapper);
        stand.addTrees(treesOfStandResolver.treesInside(stand.getProject(), polygon), Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeStandMapper.toResponse(stand);
    }
}

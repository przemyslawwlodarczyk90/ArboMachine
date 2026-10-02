package com.example.arbomachine.domain.treestand.service.treestandfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treestand.dto.TreeStandRequest;
import com.example.arbomachine.domain.treestand.dto.TreeStandResponse;
import com.example.arbomachine.domain.treestand.entity.TreeStand;
import com.example.arbomachine.domain.treestand.geometry.StandPolygon;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/** Edycja grupy (PUT - komplet: nazwa, uwagi, geometria, drzewa). Grupa i drzewa musza byc z projektu z URL. */
@Component
public class UpdateTreeStandHandler {
    private final FindTreeStandOfProjectOwnedByCurrentUserHandler findTreeStandHandler;
    private final TreesOfStandResolver treesOfStandResolver;
    private final TreeStandMapper treeStandMapper;
    private final ObjectMapper objectMapper;

    public UpdateTreeStandHandler(FindTreeStandOfProjectOwnedByCurrentUserHandler findTreeStandHandler,
                                  TreesOfStandResolver treesOfStandResolver, TreeStandMapper treeStandMapper, ObjectMapper objectMapper) {
        this.findTreeStandHandler = findTreeStandHandler;
        this.treesOfStandResolver = treesOfStandResolver;
        this.treeStandMapper = treeStandMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TreeStandResponse handle(String currentUserEmail, Long projectId, Long standId, TreeStandRequest request) {
        TreeStand stand = findTreeStandHandler.handle(currentUserEmail, projectId, standId);
        StandPolygon polygon = StandPolygon.fromGeoJson(request.geometry());
        Set<Tree> trees = treesOfStandResolver.treesOfProject(stand.getProject(), request.treeIds());
        if (Boolean.TRUE.equals(request.assignTreesInside())) trees.addAll(treesOfStandResolver.treesInside(stand.getProject(), polygon));
        stand.replace(request.name().trim(), TreeStandTexts.trimmedOrNull(request.notes()), polygon.toGeoJson(objectMapper).toString(),
                polygon.roundedAreaSquareMeters(), trees, Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeStandMapper.toResponse(stand);
    }
}

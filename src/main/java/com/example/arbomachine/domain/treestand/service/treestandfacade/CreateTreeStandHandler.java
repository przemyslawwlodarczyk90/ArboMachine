package com.example.arbomachine.domain.treestand.service.treestandfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treestand.dto.TreeStandRequest;
import com.example.arbomachine.domain.treestand.dto.TreeStandResponse;
import com.example.arbomachine.domain.treestand.entity.TreeStand;
import com.example.arbomachine.domain.treestand.entity.TreeStandSource;
import com.example.arbomachine.domain.treestand.geometry.StandPolygon;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/** Nowa grupa drzew narysowana w aplikacji (zrodlo MANUAL) w WLASNYM projekcie. */
@Component
public class CreateTreeStandHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeStandRepository treeStandRepository;
    private final TreesOfStandResolver treesOfStandResolver;
    private final TreeStandMapper treeStandMapper;
    private final ObjectMapper objectMapper;

    public CreateTreeStandHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                  TreeStandRepository treeStandRepository, TreesOfStandResolver treesOfStandResolver,
                                  TreeStandMapper treeStandMapper, ObjectMapper objectMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeStandRepository = treeStandRepository;
        this.treesOfStandResolver = treesOfStandResolver;
        this.treeStandMapper = treeStandMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TreeStandResponse handle(String currentUserEmail, Long projectId, TreeStandRequest request) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        StandPolygon polygon = StandPolygon.fromGeoJson(request.geometry());
        Set<Tree> trees = treesOfStandResolver.treesOfProject(ownedProject, request.treeIds());
        if (Boolean.TRUE.equals(request.assignTreesInside())) trees.addAll(treesOfStandResolver.treesInside(ownedProject, polygon));
        TreeStand stand = TreeStand.create(ownedProject, request.name().trim(), TreeStandTexts.trimmedOrNull(request.notes()),
                polygon.toGeoJson(objectMapper).toString(), polygon.roundedAreaSquareMeters(), TreeStandSource.MANUAL, trees,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeStandMapper.toResponse(treeStandRepository.save(stand));
    }
}

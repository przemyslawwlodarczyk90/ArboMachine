package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.tree.dto.TreeLocationRequest;
import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.entity.TreeLocation;
import com.example.arbomachine.domain.tree.entity.TreeLocationSource;
import com.example.arbomachine.domain.tree.mapper.TreeMapper;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Ustawienie albo zamiana lokalizacji drzewa w WLASNYM projekcie (najpierw wlasnosc projektu, potem drzewo
 * zawezone do tego projektu). updatedAt przesuwa sie tylko, gdy punkt albo dane pomiaru (wysokosc, dokladnosc, zrodlo) faktycznie sie zmieniaja.
 */
@Component
public class SetTreeLocationHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeRepository treeRepository;
    private final TreeMapper treeMapper;

    public SetTreeLocationHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                  TreeRepository treeRepository, TreeMapper treeMapper) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeRepository = treeRepository;
        this.treeMapper = treeMapper;
    }

    @Transactional
    public TreeResponse handle(String currentUserEmail, Long projectId, Long treeId, TreeLocationRequest treeLocationRequest) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);

        TreeLocation newLocation = TreeLocation.of(treeLocationRequest.latitude(), treeLocationRequest.longitude(),
                treeLocationRequest.altitudeM(), treeLocationRequest.accuracyM(),
                treeLocationRequest.source() == null ? null : TreeLocationSource.valueOf(treeLocationRequest.source()),
                treeLocationRequest.measuredAt(), blankToNull(treeLocationRequest.adapterCode()),
                treeLocationRequest.deviceLabel(), Instant.now());
        if (newLocation.equals(treeInOwnedProject.getLocation())) {
            return treeMapper.toTreeResponse(treeInOwnedProject);
        }

        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL (jak przy edycji drzewa).
        treeInOwnedProject.placeAt(newLocation, Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeMapper.toTreeResponse(treeRepository.save(treeInOwnedProject));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}

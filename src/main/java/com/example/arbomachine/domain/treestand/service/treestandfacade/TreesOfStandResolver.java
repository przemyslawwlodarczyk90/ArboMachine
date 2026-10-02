package com.example.arbomachine.domain.treestand.service.treestandfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treestand.exception.InvalidTreeStandException;
import com.example.arbomachine.domain.treestand.geometry.StandPolygon;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Drzewa grupy: id z zadania musza nalezec do TEGO projektu (id spoza projektu -> 400, bez zdradzania cudzych danych),
 * a "drzewa w obszarze" to drzewa projektu z lokalizacja wewnatrz wielokata.
 */
@Component
public class TreesOfStandResolver {
    private final TreeRepository treeRepository;

    public TreesOfStandResolver(TreeRepository treeRepository) {
        this.treeRepository = treeRepository;
    }

    public Set<Tree> treesOfProject(Project ownedProject, List<Long> treeIds) {
        if (treeIds == null || treeIds.isEmpty()) return new HashSet<>();
        Set<Long> requestedIds = new HashSet<>(treeIds);
        Set<Tree> trees = treeRepository.findAllById(requestedIds).stream()
                .filter(tree -> tree.getProject().getId().equals(ownedProject.getId()))
                .collect(Collectors.toSet());
        if (trees.size() != requestedIds.size()) throw new InvalidTreeStandException("Tree does not belong to the project");
        return trees;
    }

    public Set<Tree> treesInside(Project ownedProject, StandPolygon polygon) {
        return treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .filter(tree -> tree.getLocation() != null && tree.getLocation().getLatitude() != null
                        && polygon.contains(tree.getLocation().getLongitude().doubleValue(), tree.getLocation().getLatitude().doubleValue()))
                .collect(Collectors.toSet());
    }
}

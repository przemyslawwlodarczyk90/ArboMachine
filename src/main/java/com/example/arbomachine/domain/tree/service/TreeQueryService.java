package com.example.arbomachine.domain.tree.service;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.exception.TreeNotFoundException;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Odczyt drzew zawsze zawezony do projektu. Przyjmuje id projektu, ktorego wlasnosc wywolujacy
 * juz sprawdzil - sam nie zna uzytkownikow.
 */
@Service
public class TreeQueryService {
    private final TreeRepository treeRepository;

    public TreeQueryService(TreeRepository treeRepository) {
        this.treeRepository = treeRepository;
    }

    public List<Tree> findTreesOfProject(Long ownedProjectId) {
        return treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(ownedProjectId);
    }

    /** Drzewo z innego projektu i nieistniejace drzewo daja ten sam wyjatek (-> 404). */
    public Tree getTreeOfProject(Long treeId, Long ownedProjectId) {
        return treeRepository.findByIdAndProjectId(treeId, ownedProjectId)
                .orElseThrow(() -> new TreeNotFoundException("Tree not found"));
    }
}

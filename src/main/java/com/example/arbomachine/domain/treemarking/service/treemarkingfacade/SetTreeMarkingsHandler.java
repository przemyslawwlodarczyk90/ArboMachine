package com.example.arbomachine.domain.treemarking.service.treemarkingfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingCatalog;
import com.example.arbomachine.domain.treemarking.dto.TreeMarkingsRequest;
import com.example.arbomachine.domain.treemarking.dto.TreeMarkingsResponse;
import com.example.arbomachine.domain.treemarking.entity.TreeMarking;
import com.example.arbomachine.infrastructure.repository.TreeMarkingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Zastepuje KOMPLET oznaczen drzewa z wlasnego projektu: usuwa te spoza listy, dodaje brakujace (istniejace zostaja
 * z pierwotna data). Kod spoza katalogu -> 400 i nic sie nie zmienia.
 */
@Component
public class SetTreeMarkingsHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeMarkingRepository treeMarkingRepository;
    private final TreeMarkingCatalog treeMarkingCatalog;

    public SetTreeMarkingsHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                  TreeMarkingRepository treeMarkingRepository, TreeMarkingCatalog treeMarkingCatalog) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeMarkingRepository = treeMarkingRepository;
        this.treeMarkingCatalog = treeMarkingCatalog;
    }

    @Transactional
    public TreeMarkingsResponse handle(String currentUserEmail, Long projectId, Long treeId, TreeMarkingsRequest request) {
        Tree ownedTree = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        Set<String> requestedCodes = treeMarkingCatalog.requireSupportedCodes(request.codes());
        List<TreeMarking> existing = treeMarkingRepository.findAllByTreeId(ownedTree.getId());
        treeMarkingRepository.deleteAll(existing.stream().filter(marking -> !requestedCodes.contains(marking.getCode())).toList());
        // Usuniecia przed wstawieniami - unikalnosc (tree_id, code) nie moze trafic na wiersz w trakcie usuwania.
        treeMarkingRepository.flush();
        Set<String> existingCodes = existing.stream().map(TreeMarking::getCode).collect(Collectors.toSet());
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        treeMarkingRepository.saveAll(requestedCodes.stream().filter(code -> !existingCodes.contains(code))
                .map(code -> TreeMarking.create(ownedTree, code, now)).toList());
        return new TreeMarkingsResponse(ownedTree.getId(), treeMarkingCatalog.inCatalogOrder(requestedCodes));
    }
}

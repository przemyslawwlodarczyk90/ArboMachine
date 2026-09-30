package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.mapsymbol.catalog.MapSymbolCatalog;
import com.example.arbomachine.domain.mapsymbol.dto.MapSymbolRequest;
import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.mapper.TreeMapper;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Ustawienie albo zmiana symbolu drzewa na mapie w WLASNYM projekcie. Najpierw wlasnosc (404 dla cudzego),
 * potem kod z katalogu (400 dla nieznanego). updatedAt przesuwa sie tylko przy rzeczywistej zmianie.
 */
@Component
public class SetTreeMapSymbolHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final MapSymbolCatalog mapSymbolCatalog;
    private final TreeRepository treeRepository;
    private final TreeMapper treeMapper;

    public SetTreeMapSymbolHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                   MapSymbolCatalog mapSymbolCatalog, TreeRepository treeRepository, TreeMapper treeMapper) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.mapSymbolCatalog = mapSymbolCatalog;
        this.treeRepository = treeRepository;
        this.treeMapper = treeMapper;
    }

    @Transactional
    public TreeResponse handle(String currentUserEmail, Long projectId, Long treeId, MapSymbolRequest mapSymbolRequest) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        String supportedSymbolCode = mapSymbolCatalog.requireSupportedCode(mapSymbolRequest.symbolCode());
        if (supportedSymbolCode.equals(treeInOwnedProject.getMapSymbolCode())) {
            return treeMapper.toTreeResponse(treeInOwnedProject);
        }
        treeInOwnedProject.assignMapSymbol(supportedSymbolCode, Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeMapper.toTreeResponse(treeRepository.save(treeInOwnedProject));
    }
}

package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.mapsymbol.catalog.MapSymbolCatalog;
import com.example.arbomachine.domain.mapsymbol.dto.MapSymbolRequest;
import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.treework.mapper.TreeWorkMapper;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Ustawienie albo zmiana symbolu pracy na mapie - bez edycji nazwy i opisu. Cala hierarchia (wlasny projekt ->
 * drzewo -> praca tego drzewa) przed walidacja kodu; kod spoza katalogu -> 400.
 */
@Component
public class SetTreeWorkMapSymbolHandler {
    private final FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler;
    private final MapSymbolCatalog mapSymbolCatalog;
    private final TreeWorkRepository treeWorkRepository;
    private final TreeWorkMapper treeWorkMapper;

    public SetTreeWorkMapSymbolHandler(
            FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler,
            MapSymbolCatalog mapSymbolCatalog, TreeWorkRepository treeWorkRepository, TreeWorkMapper treeWorkMapper) {
        this.findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler = findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler;
        this.mapSymbolCatalog = mapSymbolCatalog;
        this.treeWorkRepository = treeWorkRepository;
        this.treeWorkMapper = treeWorkMapper;
    }

    @Transactional
    public TreeWorkResponse handle(String currentUserEmail, Long projectId, Long treeId, Long treeWorkId,
                                   MapSymbolRequest mapSymbolRequest) {
        TreeWork treeWork = findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId,
                treeId, treeWorkId);
        String supportedSymbolCode = mapSymbolCatalog.requireSupportedCode(mapSymbolRequest.symbolCode());
        if (supportedSymbolCode.equals(treeWork.getMapSymbolCode())) {
            return treeWorkMapper.toTreeWorkResponse(treeWork);
        }
        treeWork.assignMapSymbol(supportedSymbolCode, Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeWorkMapper.toTreeWorkResponse(treeWorkRepository.save(treeWork));
    }
}

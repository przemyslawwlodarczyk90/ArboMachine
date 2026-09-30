package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.treework.mapper.TreeWorkMapper;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Usuniecie symbolu pracy (praca nie pokazuje sie jako znaczek na mapie); bez symbolu - bez zmian. */
@Component
public class ClearTreeWorkMapSymbolHandler {
    private final FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler;
    private final TreeWorkRepository treeWorkRepository;
    private final TreeWorkMapper treeWorkMapper;

    public ClearTreeWorkMapSymbolHandler(
            FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler,
            TreeWorkRepository treeWorkRepository, TreeWorkMapper treeWorkMapper) {
        this.findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler = findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler;
        this.treeWorkRepository = treeWorkRepository;
        this.treeWorkMapper = treeWorkMapper;
    }

    @Transactional
    public TreeWorkResponse handle(String currentUserEmail, Long projectId, Long treeId, Long treeWorkId) {
        TreeWork treeWork = findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId,
                treeId, treeWorkId);
        if (treeWork.getMapSymbolCode() == null) {
            return treeWorkMapper.toTreeWorkResponse(treeWork);
        }
        treeWork.clearMapSymbol(Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeWorkMapper.toTreeWorkResponse(treeWorkRepository.save(treeWork));
    }
}

package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.dto.UpdateTreeWorkRequest;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.treework.mapper.TreeWorkMapper;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Edycja pracy - komplet pol (nazwa, opis, status, priorytet, terminy; etap 28: dowolna zmiana statusu, bez regul
 * przejsc). Praca musi nalezec dokladnie do drzewa z URL (a drzewo do wlasnego projektu).
 * Drzewa pracy nie da sie zmienic. updatedAt przesuwa sie tylko przy rzeczywistej zmianie.
 */
@Component
public class UpdateTreeWorkHandler {
    private final FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler;
    private final TreeWorkRepository treeWorkRepository;
    private final TreeWorkMapper treeWorkMapper;

    public UpdateTreeWorkHandler(
            FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler,
            TreeWorkRepository treeWorkRepository, TreeWorkMapper treeWorkMapper) {
        this.findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler = findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler;
        this.treeWorkRepository = treeWorkRepository;
        this.treeWorkMapper = treeWorkMapper;
    }

    @Transactional
    public TreeWorkResponse handle(String currentUserEmail, Long projectId, Long treeId, Long treeWorkId,
                                   UpdateTreeWorkRequest updateTreeWorkRequest) {
        TreeWork treeWork = findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId,
                treeId, treeWorkId);
        TreeWork.Fields fields = TreeWorkFieldsNormalizer.normalizedFieldsOf(updateTreeWorkRequest);
        if (!treeWork.differsFrom(fields)) {
            return treeWorkMapper.toTreeWorkResponse(treeWork);
        }
        treeWork.replaceFields(fields, Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeWorkMapper.toTreeWorkResponse(treeWorkRepository.save(treeWork));
    }
}

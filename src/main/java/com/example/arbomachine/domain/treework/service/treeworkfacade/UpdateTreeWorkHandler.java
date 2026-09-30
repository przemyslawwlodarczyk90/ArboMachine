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
 * Edycja nazwy i opisu pracy. Praca musi nalezec dokladnie do drzewa z URL (a drzewo do wlasnego projektu).
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
        TreeWorkFieldsNormalizer.NormalizedTreeWorkFields fields =
                TreeWorkFieldsNormalizer.normalizedFieldsOf(updateTreeWorkRequest);
        if (!treeWork.differsFrom(fields.name(), fields.description())) {
            return treeWorkMapper.toTreeWorkResponse(treeWork);
        }
        treeWork.replaceNameAndDescription(fields.name(), fields.description(),
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeWorkMapper.toTreeWorkResponse(treeWorkRepository.save(treeWork));
    }
}

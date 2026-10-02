package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.treework.entity.TreeWorkPriority;
import com.example.arbomachine.domain.treework.entity.TreeWorkStatus;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.mapper.TreeWorkMapper;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Prace wszystkich drzew WLASNEGO projektu - mapa projektu i widok "Gospodarka drzewostanem" (etap 28: opcjonalny
 * filtr statusu i priorytetu) jednym zapytaniem zamiast osobnego zapytania o kazde drzewo. Wlasnosc sprawdzana raz, na projekcie; cudzy projekt -> 404.
 */
@Component
public class GetTreeWorksOfProjectHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeWorkRepository treeWorkRepository;
    private final TreeWorkMapper treeWorkMapper;

    public GetTreeWorksOfProjectHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                        TreeWorkRepository treeWorkRepository, TreeWorkMapper treeWorkMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeWorkRepository = treeWorkRepository;
        this.treeWorkMapper = treeWorkMapper;
    }

    /** Puste zbiory statusow / priorytetow = bez filtra. Filtr w pamieci - projekt ma co najwyzej setki prac (V1). */
    public List<TreeWorkResponse> handle(String currentUserEmail, Long projectId, Set<TreeWorkStatus> statuses,
                                         Set<TreeWorkPriority> priorities) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeWorkRepository.findAllByTreeProjectIdOrderByTreeIdAscCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .filter(treeWork -> statuses.isEmpty() || statuses.contains(treeWork.getStatus()))
                .filter(treeWork -> priorities.isEmpty() || (treeWork.getPriority() != null && priorities.contains(treeWork.getPriority())))
                .map(treeWorkMapper::toTreeWorkResponse)
                .toList();
    }
}

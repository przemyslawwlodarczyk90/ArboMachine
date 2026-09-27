package com.example.arbomachine.domain.tree.service;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.ProjectQueryService;
import com.example.arbomachine.domain.tree.dto.CreateTreeRequest;
import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.mapper.TreeMapper;
import com.example.arbomachine.domain.user.service.UserAccountQueryService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Punkt wejscia modulu drzew (odpowiednik ProjectService). Kazda operacja zaczyna sie od pobrania projektu
 * WLASNOSCIA zalogowanego uzytkownika przez ProjectQueryService - cudzy albo nieistniejacy projekt
 * konczy sie ProjectNotFoundException (404), zanim dotkniemy jakiegokolwiek drzewa.
 * Nie zawiera logiki biznesowej - deleguje do serwisow jednego zadania.
 */
@Service
public class TreeService {
    private final UserAccountQueryService userAccountQueryService;
    private final ProjectQueryService projectQueryService;
    private final TreeCreationService treeCreationService;
    private final TreeQueryService treeQueryService;
    private final TreeMapper treeMapper;

    public TreeService(UserAccountQueryService userAccountQueryService,
                       ProjectQueryService projectQueryService,
                       TreeCreationService treeCreationService,
                       TreeQueryService treeQueryService,
                       TreeMapper treeMapper) {
        this.userAccountQueryService = userAccountQueryService;
        this.projectQueryService = projectQueryService;
        this.treeCreationService = treeCreationService;
        this.treeQueryService = treeQueryService;
        this.treeMapper = treeMapper;
    }

    @Transactional
    public TreeResponse createTreeInProjectOfCurrentUser(String currentUserEmail, Long projectId,
                                                         CreateTreeRequest createTreeRequest) {
        Project ownedProject = getProjectOwnedByCurrentUser(currentUserEmail, projectId);
        return treeMapper.toTreeResponse(treeCreationService.createTreeInProject(ownedProject, createTreeRequest));
    }

    @Transactional
    public List<TreeResponse> getTreesOfProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        Project ownedProject = getProjectOwnedByCurrentUser(currentUserEmail, projectId);
        return treeQueryService.findTreesOfProject(ownedProject.getId()).stream()
                .map(treeMapper::toTreeResponse)
                .toList();
    }

    @Transactional
    public TreeResponse getTreeOfProjectOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        Project ownedProject = getProjectOwnedByCurrentUser(currentUserEmail, projectId);
        return treeMapper.toTreeResponse(treeQueryService.getTreeOfProject(treeId, ownedProject.getId()));
    }

    private Project getProjectOwnedByCurrentUser(String currentUserEmail, Long projectId) {
        Long currentUserAccountId = userAccountQueryService.getUserAccountEntityByEmail(currentUserEmail).getId();
        return projectQueryService.getProjectOwnedBy(projectId, currentUserAccountId);
    }
}

package com.example.arbomachine.domain.project.service;

import com.example.arbomachine.domain.project.dto.CreateProjectRequest;
import com.example.arbomachine.domain.project.dto.ProjectResponse;
import com.example.arbomachine.domain.project.dto.UpdateProjectRequest;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.mapper.ProjectMapper;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.UserAccountQueryService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Punkt wejscia modulu projektow (odpowiednik UserAccountFacade): zamienia e-mail zalogowanego uzytkownika
 * (z tokenu JWT) na konto-wlasciciela i deleguje do serwisow jednego zadania. Nie zawiera logiki biznesowej.
 * <p>
 * Kazda metoda przyjmuje e-mail biezacego uzytkownika - wlasciciel NIGDY nie pochodzi z requestu.
 */
@Service
public class ProjectService {
    private final UserAccountQueryService userAccountQueryService;
    private final ProjectCreationService projectCreationService;
    private final ProjectQueryService projectQueryService;
    private final ProjectUpdateService projectUpdateService;
    private final ProjectMapper projectMapper;

    public ProjectService(UserAccountQueryService userAccountQueryService,
                          ProjectCreationService projectCreationService,
                          ProjectQueryService projectQueryService,
                          ProjectUpdateService projectUpdateService,
                          ProjectMapper projectMapper) {
        this.userAccountQueryService = userAccountQueryService;
        this.projectCreationService = projectCreationService;
        this.projectQueryService = projectQueryService;
        this.projectUpdateService = projectUpdateService;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public ProjectResponse createProjectForCurrentUser(String currentUserEmail, CreateProjectRequest createProjectRequest) {
        UserAccount currentUserAccount = userAccountQueryService.getUserAccountEntityByEmail(currentUserEmail);
        Project createdProject = projectCreationService.createProjectOwnedBy(currentUserAccount, createProjectRequest);
        return projectMapper.toProjectResponse(createdProject);
    }

    @Transactional
    public List<ProjectResponse> getProjectsOfCurrentUser(String currentUserEmail) {
        Long currentUserAccountId = resolveCurrentUserAccountId(currentUserEmail);
        return projectQueryService.findProjectsOwnedBy(currentUserAccountId).stream()
                .map(projectMapper::toProjectResponse)
                .toList();
    }

    @Transactional
    public ProjectResponse getProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        Long currentUserAccountId = resolveCurrentUserAccountId(currentUserEmail);
        return projectMapper.toProjectResponse(projectQueryService.getProjectOwnedBy(projectId, currentUserAccountId));
    }

    @Transactional
    public ProjectResponse updateProjectOfCurrentUser(String currentUserEmail, Long projectId,
                                                      UpdateProjectRequest updateProjectRequest) {
        Long currentUserAccountId = resolveCurrentUserAccountId(currentUserEmail);
        Project ownedProject = projectQueryService.getProjectOwnedBy(projectId, currentUserAccountId);
        return projectMapper.toProjectResponse(projectUpdateService.applyChanges(ownedProject, updateProjectRequest));
    }

    private Long resolveCurrentUserAccountId(String currentUserEmail) {
        return userAccountQueryService.getUserAccountEntityByEmail(currentUserEmail).getId();
    }
}

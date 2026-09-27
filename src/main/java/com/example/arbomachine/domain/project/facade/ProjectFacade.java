package com.example.arbomachine.domain.project.facade;

import com.example.arbomachine.domain.project.dto.CreateProjectRequest;
import com.example.arbomachine.domain.project.dto.ProjectResponse;
import com.example.arbomachine.domain.project.dto.UpdateProjectRequest;
import com.example.arbomachine.domain.project.service.projectfacade.CreateProjectHandler;
import com.example.arbomachine.domain.project.service.projectfacade.GetProjectOfCurrentUserHandler;
import com.example.arbomachine.domain.project.service.projectfacade.GetProjectsOfCurrentUserHandler;
import com.example.arbomachine.domain.project.service.projectfacade.UpdateProjectHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Projekty zalogowanego uzytkownika. Kazda metoda przyjmuje e-mail z tokenu JWT - wlasciciel NIGDY nie pochodzi
 * z requestu. Tylko deleguje do handlerow z service/projectfacade.
 */
@Component
public class ProjectFacade {
    private final CreateProjectHandler createProjectHandler;
    private final GetProjectsOfCurrentUserHandler getProjectsOfCurrentUserHandler;
    private final GetProjectOfCurrentUserHandler getProjectOfCurrentUserHandler;
    private final UpdateProjectHandler updateProjectHandler;

    public ProjectFacade(CreateProjectHandler createProjectHandler,
                         GetProjectsOfCurrentUserHandler getProjectsOfCurrentUserHandler,
                         GetProjectOfCurrentUserHandler getProjectOfCurrentUserHandler,
                         UpdateProjectHandler updateProjectHandler) {
        this.createProjectHandler = createProjectHandler;
        this.getProjectsOfCurrentUserHandler = getProjectsOfCurrentUserHandler;
        this.getProjectOfCurrentUserHandler = getProjectOfCurrentUserHandler;
        this.updateProjectHandler = updateProjectHandler;
    }

    public ProjectResponse createProjectForCurrentUser(String currentUserEmail, CreateProjectRequest createProjectRequest) {
        return createProjectHandler.handle(currentUserEmail, createProjectRequest);
    }

    public List<ProjectResponse> getProjectsOfCurrentUser(String currentUserEmail) {
        return getProjectsOfCurrentUserHandler.handle(currentUserEmail);
    }

    public ProjectResponse getProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        return getProjectOfCurrentUserHandler.handle(currentUserEmail, projectId);
    }

    public ProjectResponse updateProjectOfCurrentUser(String currentUserEmail, Long projectId,
                                                      UpdateProjectRequest updateProjectRequest) {
        return updateProjectHandler.handle(currentUserEmail, projectId, updateProjectRequest);
    }
}

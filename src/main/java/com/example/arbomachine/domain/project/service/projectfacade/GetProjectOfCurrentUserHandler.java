package com.example.arbomachine.domain.project.service.projectfacade;

import com.example.arbomachine.domain.project.dto.ProjectResponse;
import com.example.arbomachine.domain.project.mapper.ProjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

/** Projekt po id - tylko wlasny; cudzy albo nieistniejacy konczy sie 404. */
@Component
public class GetProjectOfCurrentUserHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectMapper projectMapper;

    public GetProjectOfCurrentUserHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                          ProjectMapper projectMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public ProjectResponse handle(String currentUserEmail, Long projectId) {
        return projectMapper.toProjectResponse(findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId));
    }
}

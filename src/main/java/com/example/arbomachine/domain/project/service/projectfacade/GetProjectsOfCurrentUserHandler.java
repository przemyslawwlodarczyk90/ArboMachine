package com.example.arbomachine.domain.project.service.projectfacade;

import com.example.arbomachine.domain.project.dto.ProjectResponse;
import com.example.arbomachine.domain.project.mapper.ProjectMapper;
import com.example.arbomachine.domain.user.service.useraccountfacade.FindUserAccountByEmailHandler;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.List;

/** Wylacznie projekty zalogowanego uzytkownika, najnowsze pierwsze. */
@Component
public class GetProjectsOfCurrentUserHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    public GetProjectsOfCurrentUserHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                           ProjectRepository projectRepository, ProjectMapper projectMapper) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public List<ProjectResponse> handle(String currentUserEmail) {
        Long currentUserAccountId = findUserAccountByEmailHandler.handle(currentUserEmail).getId();
        return projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(currentUserAccountId).stream()
                .map(projectMapper::toProjectResponse)
                .toList();
    }
}

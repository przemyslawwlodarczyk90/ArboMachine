package com.example.arbomachine.domain.project.service;

import com.example.arbomachine.domain.project.dto.CreateProjectRequest;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Zakladanie nowego projektu dla podanego wlasciciela. Wlasciciela ustala wywolujacy, nigdy request. */
@Service
public class ProjectCreationService {
    private final ProjectRepository projectRepository;

    public ProjectCreationService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project createProjectOwnedBy(UserAccount owner, CreateProjectRequest createProjectRequest) {
        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL, wiec odpowiedz POST
        // zwraca dokladnie te same daty, ktore pozniej odczyta GET.
        Instant creationTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Project newProject = Project.createOwnedBy(owner, createProjectRequest.name(),
                createProjectRequest.description(), creationTime);
        return projectRepository.save(newProject);
    }
}

package com.example.arbomachine.domain.project.service.projectfacade;

import com.example.arbomachine.domain.project.dto.CreateProjectRequest;
import com.example.arbomachine.domain.project.dto.ProjectResponse;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.mapper.ProjectMapper;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.useraccountfacade.FindUserAccountByEmailHandler;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Nowy projekt - wlascicielem zostaje zalogowany uzytkownik (z tokenu JWT), nigdy wartosc z requestu. */
@Component
public class CreateProjectHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    public CreateProjectHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                ProjectRepository projectRepository, ProjectMapper projectMapper) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public ProjectResponse handle(String currentUserEmail, CreateProjectRequest createProjectRequest) {
        UserAccount projectOwner = findUserAccountByEmailHandler.handle(currentUserEmail);
        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL, wiec odpowiedz POST
        // zwraca dokladnie te same daty, ktore pozniej odczyta GET.
        Instant creationTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Project newProject = Project.createOwnedBy(projectOwner, createProjectRequest.name(),
                createProjectRequest.description(), creationTime);
        newProject.setClientName(ProjectOrderFields.trimmedOrNull(createProjectRequest.clientName()));
        newProject.setSiteAddress(ProjectOrderFields.trimmedOrNull(createProjectRequest.siteAddress()));
        newProject.setOrderDate(createProjectRequest.orderDate());
        newProject.setContractNumber(ProjectOrderFields.trimmedOrNull(createProjectRequest.contractNumber()));
        newProject.setDocumentPurpose(ProjectOrderFields.trimmedOrNull(createProjectRequest.documentPurpose()));
        return projectMapper.toProjectResponse(projectRepository.save(newProject));
    }
}

package com.example.arbomachine.domain.project.service.projectfacade;

import com.example.arbomachine.domain.project.dto.ProjectResponse;
import com.example.arbomachine.domain.project.dto.UpdateProjectRequest;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.mapper.ProjectMapper;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateProjectHandlerTest {

    private static final Instant CREATION_TIME = Instant.parse("2026-01-01T10:00:00Z");
    private static final String OWNER_EMAIL = "owner@example.com";

    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler = mock(FindProjectOwnedByCurrentUserHandler.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final UpdateProjectHandler updateProjectHandler =
            new UpdateProjectHandler(findProjectOwnedByCurrentUserHandler, projectRepository, new ProjectMapper());
    private final UserAccount projectOwner = UserAccount.builder().id(1L).email(OWNER_EMAIL).build();
    private Project ownedProject;

    @BeforeEach
    void givenOwnedProject() {
        ownedProject = Project.createOwnedBy(projectOwner, "Stara nazwa", "Stary opis", CREATION_TIME);
        when(findProjectOwnedByCurrentUserHandler.handle(OWNER_EMAIL, 5L)).thenReturn(ownedProject);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void changesNameAndDescriptionAndRefreshesUpdatedAt() {
        ProjectResponse updatedProject = updateProjectHandler.handle(OWNER_EMAIL, 5L, new UpdateProjectRequest("Nowa", "Nowy opis"));

        assertThat(updatedProject.name()).isEqualTo("Nowa");
        assertThat(updatedProject.description()).isEqualTo("Nowy opis");
        assertThat(updatedProject.updatedAt()).isAfter(CREATION_TIME);
    }

    @Test
    void omittedFieldsStayUnchanged() {
        ProjectResponse updatedProject = updateProjectHandler.handle(OWNER_EMAIL, 5L, new UpdateProjectRequest(null, null));

        assertThat(updatedProject.name()).isEqualTo("Stara nazwa");
        assertThat(updatedProject.description()).isEqualTo("Stary opis");
    }

    @Test
    void ownerAndCreatedAtAreNeverTouched() {
        updateProjectHandler.handle(OWNER_EMAIL, 5L, new UpdateProjectRequest("Nowa", null));

        assertThat(ownedProject.getOwner()).isSameAs(projectOwner);
        assertThat(ownedProject.getCreatedAt()).isEqualTo(CREATION_TIME);
    }
}

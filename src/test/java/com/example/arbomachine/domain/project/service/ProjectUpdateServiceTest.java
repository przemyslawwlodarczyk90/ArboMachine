package com.example.arbomachine.domain.project.service;

import com.example.arbomachine.domain.project.dto.UpdateProjectRequest;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectUpdateServiceTest {

    private static final Instant CREATION_TIME = Instant.parse("2026-01-01T10:00:00Z");

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectUpdateService projectUpdateService = new ProjectUpdateService(projectRepository);
    private final UserAccount projectOwner = UserAccount.builder().id(1L).build();
    private Project ownedProject;

    @BeforeEach
    void createOwnedProject() {
        ownedProject = Project.createOwnedBy(projectOwner, "Stara nazwa", "Stary opis", CREATION_TIME);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void changesNameAndDescriptionAndRefreshesUpdatedAt() {
        Project updatedProject = projectUpdateService.applyChanges(ownedProject, new UpdateProjectRequest("Nowa", "Nowy opis"));

        assertThat(updatedProject.getName()).isEqualTo("Nowa");
        assertThat(updatedProject.getDescription()).isEqualTo("Nowy opis");
        assertThat(updatedProject.getUpdatedAt()).isAfter(CREATION_TIME);
    }

    @Test
    void omittedFieldsStayUnchanged() {
        Project updatedProject = projectUpdateService.applyChanges(ownedProject, new UpdateProjectRequest(null, null));

        assertThat(updatedProject.getName()).isEqualTo("Stara nazwa");
        assertThat(updatedProject.getDescription()).isEqualTo("Stary opis");
    }

    @Test
    void ownerAndCreatedAtAreNeverTouched() {
        Project updatedProject = projectUpdateService.applyChanges(ownedProject, new UpdateProjectRequest("Nowa", null));

        assertThat(updatedProject.getOwner()).isSameAs(projectOwner);
        assertThat(updatedProject.getCreatedAt()).isEqualTo(CREATION_TIME);
    }
}

package com.example.arbomachine.domain.project.service.projectfacade;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountDeletionBlockedException;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectOwnerAccountDeletionGuardTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectOwnerAccountDeletionGuard projectOwnerAccountDeletionGuard =
            new ProjectOwnerAccountDeletionGuard(projectRepository);

    @Test
    void blocksDeletionOfAccountOwningProjects() {
        when(projectRepository.existsByOwnerId(1L)).thenReturn(true);

        assertThrows(UserAccountDeletionBlockedException.class,
                () -> projectOwnerAccountDeletionGuard.ensureUserAccountCanBeDeleted(UserAccount.builder().id(1L).build()));
    }

    @Test
    void allowsDeletionOfAccountWithoutProjects() {
        when(projectRepository.existsByOwnerId(2L)).thenReturn(false);

        assertDoesNotThrow(
                () -> projectOwnerAccountDeletionGuard.ensureUserAccountCanBeDeleted(UserAccount.builder().id(2L).build()));
    }
}

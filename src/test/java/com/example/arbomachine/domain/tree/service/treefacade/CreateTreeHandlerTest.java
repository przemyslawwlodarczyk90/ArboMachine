package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.tree.dto.CreateTreeRequest;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.mapper.TreeMapper;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CreateTreeHandlerTest {

    private static final String OWNER_EMAIL = "owner@example.com";

    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler = mock(FindProjectOwnedByCurrentUserHandler.class);
    private final TreeRepository treeRepository = mock(TreeRepository.class);
    private final CreateTreeHandler createTreeHandler =
            new CreateTreeHandler(findProjectOwnedByCurrentUserHandler, treeRepository, new TreeMapper());
    private final Project ownedProject = Project.createOwnedBy(UserAccount.builder().id(1L).build(), "Projekt", null,
            Instant.parse("2026-01-01T00:00:00Z"));

    @BeforeEach
    void givenOwnedProject() {
        when(findProjectOwnedByCurrentUserHandler.handle(OWNER_EMAIL, 5L)).thenReturn(ownedProject);
        when(treeRepository.save(any(Tree.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Tree savedTree() {
        ArgumentCaptor<Tree> savedTreeCaptor = ArgumentCaptor.forClass(Tree.class);
        verify(treeRepository).save(savedTreeCaptor.capture());
        return savedTreeCaptor.getValue();
    }

    @Test
    void treeBelongsToOwnedProjectAndHasEqualCreationAndUpdateTime() {
        createTreeHandler.handle(OWNER_EMAIL, 5L, new CreateTreeRequest("1", "Quercus robur", "Example field note."));

        Tree createdTree = savedTree();
        assertThat(createdTree.getProject()).isSameAs(ownedProject);
        assertThat(createdTree.getSpeciesLatin()).isEqualTo("Quercus robur");
        assertThat(createdTree.getNotes()).isEqualTo("Example field note.");
        assertThat(createdTree.getCreatedAt()).isNotNull().isEqualTo(createdTree.getUpdatedAt());
    }

    @Test
    void trimsInventoryNumberAndStoresBlankOptionalFieldsAsNull() {
        createTreeHandler.handle(OWNER_EMAIL, 5L, new CreateTreeRequest("  A-12 ", "  ", ""));

        Tree createdTree = savedTree();
        assertThat(createdTree.getInventoryNumber()).isEqualTo("A-12");
        assertThat(createdTree.getSpeciesLatin()).isNull();
        assertThat(createdTree.getNotes()).isNull();
    }
}

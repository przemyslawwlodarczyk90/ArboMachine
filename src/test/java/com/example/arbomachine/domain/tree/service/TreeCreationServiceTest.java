package com.example.arbomachine.domain.tree.service;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.tree.dto.CreateTreeRequest;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TreeCreationServiceTest {

    private final TreeRepository treeRepository = mock(TreeRepository.class);
    private final TreeCreationService treeCreationService = new TreeCreationService(treeRepository);
    private final Project ownedProject = Project.createOwnedBy(UserAccount.builder().id(1L).build(), "Projekt", null,
            Instant.parse("2026-01-01T00:00:00Z"));

    @BeforeEach
    void saveReturnsTheSameTree() {
        when(treeRepository.save(any(Tree.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void treeBelongsToGivenProjectAndHasEqualCreationAndUpdateTime() {
        Tree createdTree = treeCreationService.createTreeInProject(ownedProject,
                new CreateTreeRequest("1", "Quercus robur", "Example field note."));

        assertThat(createdTree.getProject()).isSameAs(ownedProject);
        assertThat(createdTree.getSpeciesLatin()).isEqualTo("Quercus robur");
        assertThat(createdTree.getNotes()).isEqualTo("Example field note.");
        assertThat(createdTree.getCreatedAt()).isNotNull().isEqualTo(createdTree.getUpdatedAt());
    }

    @Test
    void trimsInventoryNumberAndStoresBlankOptionalFieldsAsNull() {
        Tree createdTree = treeCreationService.createTreeInProject(ownedProject,
                new CreateTreeRequest("  A-12 ", "  ", ""));

        assertThat(createdTree.getInventoryNumber()).isEqualTo("A-12");
        assertThat(createdTree.getSpeciesLatin()).isNull();
        assertThat(createdTree.getNotes()).isNull();
    }
}

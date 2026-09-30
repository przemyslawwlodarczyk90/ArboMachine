package com.example.arbomachine.domain.treework.dto;

import com.example.arbomachine.domain.treework.entity.TreeWork;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body PATCH /api/projects/{projectId}/trees/{treeId}/works/{workId} - jak przy edycji drzewa wysylany jest
 * komplet pol: pominiety/pusty opis oznacza jego wyczyszczenie.
 */
public record UpdateTreeWorkRequest(
        @NotBlank(message = "Work name is mandatory")
        @Size(max = TreeWork.MAXIMUM_NAME_LENGTH, message = "Work name must have at most 255 characters")
        String name,

        @Size(max = TreeWork.MAXIMUM_DESCRIPTION_LENGTH, message = "Work description must have at most 5000 characters")
        String description
) implements TreeWorkFieldsRequest {}

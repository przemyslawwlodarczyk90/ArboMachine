package com.example.arbomachine.domain.tree.mapper;

import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.entity.Tree;
import org.springframework.stereotype.Component;

/** Encja Tree -> TreeResponse. Z projektu bierze tylko id (nie inicjalizuje leniwie ladowanego projektu). */
@Component
public class TreeMapper {

    public TreeResponse toTreeResponse(Tree tree) {
        return new TreeResponse(
                tree.getId(),
                tree.getProject().getId(),
                tree.getInventoryNumber(),
                tree.getSpeciesLatin(),
                tree.getSpeciesPolish(),
                tree.getTrunkCircumferenceCm(),
                tree.getHeightM(),
                tree.getNotes(),
                tree.getCreatedAt(),
                tree.getUpdatedAt()
        );
    }
}

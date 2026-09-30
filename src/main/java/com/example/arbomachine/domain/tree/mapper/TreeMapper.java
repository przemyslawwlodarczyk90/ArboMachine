package com.example.arbomachine.domain.tree.mapper;

import com.example.arbomachine.domain.tree.dto.TreeLocationResponse;
import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.entity.TreeLocation;
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
                tree.getCrownDiameterM(),
                tree.getNotes(),
                tree.getConditionNotes(),
                tree.getHazardNotes(),
                toTreeLocationResponseOrNull(tree.getLocation()),
                tree.getMapSymbolCode(),
                tree.getCreatedAt(),
                tree.getUpdatedAt()
        );
    }

    private TreeLocationResponse toTreeLocationResponseOrNull(TreeLocation location) {
        return location == null ? null : new TreeLocationResponse(location.getLatitude(), location.getLongitude());
    }
}

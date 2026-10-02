package com.example.arbomachine.domain.treework.mapper;

import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import org.springframework.stereotype.Component;

/** Encja TreeWork -> TreeWorkResponse. Z drzewa bierze tylko id (nie inicjalizuje leniwie ladowanego drzewa). */
@Component
public class TreeWorkMapper {

    public TreeWorkResponse toTreeWorkResponse(TreeWork treeWork) {
        return new TreeWorkResponse(
                treeWork.getId(),
                treeWork.getTree().getId(),
                treeWork.getName(),
                treeWork.getDescription(),
                treeWork.getMapSymbolCode(),
                treeWork.getStatus().name(),
                treeWork.getPriority() == null ? null : treeWork.getPriority().name(),
                treeWork.getPlannedDate(),
                treeWork.getCompletedDate(),
                treeWork.getCreatedAt(),
                treeWork.getUpdatedAt()
        );
    }
}

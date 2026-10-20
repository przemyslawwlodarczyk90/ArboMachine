package com.example.arbomachine.domain.treemarking.service.treemarkingfacade;

import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingCatalog;
import com.example.arbomachine.domain.treemarking.dto.TreeMarkingDefinitionResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/** Katalog oznaczen V1 w stalej kolejnosci. */
@Component
public class GetTreeMarkingCatalogHandler {
    private final TreeMarkingCatalog treeMarkingCatalog;

    public GetTreeMarkingCatalogHandler(TreeMarkingCatalog treeMarkingCatalog) {
        this.treeMarkingCatalog = treeMarkingCatalog;
    }

    public List<TreeMarkingDefinitionResponse> handle() {
        return treeMarkingCatalog.allMarkings().stream()
                .map(marking -> new TreeMarkingDefinitionResponse(marking.code(), marking.displayName(),
                        marking.category().name(), marking.category().displayName(), marking.shortLabel(), marking.color()))
                .toList();
    }
}

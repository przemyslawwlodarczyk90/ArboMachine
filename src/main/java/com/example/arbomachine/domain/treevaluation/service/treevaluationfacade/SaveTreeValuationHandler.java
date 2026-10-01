package com.example.arbomachine.domain.treevaluation.service.treevaluationfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treevaluation.dto.TreeValuationRequest;
import com.example.arbomachine.domain.treevaluation.dto.TreeValuationResponse;
import com.example.arbomachine.domain.treevaluation.entity.TreeValuation;
import com.example.arbomachine.domain.treevaluation.exception.EmptyTreeValuationException;
import com.example.arbomachine.infrastructure.repository.TreeValuationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Zapis waloryzacji (PUT = utworz albo zastap cala ocene) dla drzewa z WLASNEGO projektu. Bez zadnych obliczen -
 * zapisujemy ocene arborysty. Pusta ocena -> 400 (do usuniecia sluzy DELETE). PUT jest idempotentny, wiec
 * powtorzona synchronizacja offline nie tworzy duplikatow.
 */
@Component
public class SaveTreeValuationHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeValuationRepository treeValuationRepository;

    public SaveTreeValuationHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                    TreeValuationRepository treeValuationRepository) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeValuationRepository = treeValuationRepository;
    }

    @Transactional
    public TreeValuationResponse handle(String currentUserEmail, Long projectId, Long treeId, TreeValuationRequest request) {
        Tree tree = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        TreeValuation.Assessment assessment = TreeValuationMapper.assessmentOf(request);
        if (assessment.isEmpty()) throw new EmptyTreeValuationException("Valuation must contain at least one value");
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        TreeValuation valuation = treeValuationRepository.findByTreeId(tree.getId())
                .map(existing -> {
                    existing.replaceAssessment(assessment, now);
                    return existing;
                })
                .orElseGet(() -> TreeValuation.createForTree(tree, assessment, now));
        return TreeValuationMapper.toResponse(treeValuationRepository.save(valuation));
    }
}

package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.treevaluation.entity.TreeValuation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Waloryzacje zawsze w kontekscie drzewa - wlasnosc projektu i drzewa sprawdza wczesniej
 * FindTreeInProjectOwnedByCurrentUserHandler. Nigdy wyszukiwanie po samym id waloryzacji.
 */
@Repository
public interface TreeValuationRepository extends JpaRepository<TreeValuation, Long> {

    Optional<TreeValuation> findByTreeId(Long treeId);

    /** Waloryzacje wszystkich drzew projektu jednym zapytaniem (lista drzew, mapa, dokument, eksport). */
    List<TreeValuation> findAllByTreeProjectId(Long projectId);
}

package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.treestand.entity.TreeStand;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Grupy drzew zawsze w kontekscie projektu - wlasnosc projektu sprawdza wczesniej handler. */
@Repository
public interface TreeStandRepository extends JpaRepository<TreeStand, Long> {

    /** Grupy projektu z przypisanymi drzewami jednym zapytaniem (lista, mapa, eksport, dokument). */
    @EntityGraph(attributePaths = "trees")
    List<TreeStand> findAllByProjectIdOrderByNameAscIdAsc(Long projectId);

    /** Pusty wynik takze dla grupy innego projektu. */
    @EntityGraph(attributePaths = "trees")
    Optional<TreeStand> findByIdAndProjectId(Long standId, Long projectId);

    long countByProjectId(Long projectId);
}

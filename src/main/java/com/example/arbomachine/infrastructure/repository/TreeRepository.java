package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.tree.entity.Tree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Drzewa zawsze pobieramy w kontekscie projektu. Wlasnosc projektu musi byc sprawdzona WCZESNIEJ
 * (FindProjectOwnedByCurrentUserHandler) - to repozytorium zaweza tylko do projektu.
 */
@Repository
public interface TreeRepository extends JpaRepository<Tree, Long> {

    /** Drzewa projektu w kolejnosci dodawania. */
    List<Tree> findAllByProjectIdOrderByCreatedAtAscIdAsc(Long projectId);

    /** Pusty wynik takze dla drzewa, ktore istnieje, ale nalezy do innego projektu. */
    Optional<Tree> findByIdAndProjectId(Long treeId, Long projectId);

    /** Liczba drzew projektu - COUNT w bazie (podsumowanie projektu). */
    long countByProjectId(Long projectId);

    /** Liczba drzew projektu z lokalizacja (lokalizacja to zawsze komplet pary, wiec wystarczy latitude). */
    long countByProjectIdAndLocationLatitudeIsNotNull(Long projectId);
}

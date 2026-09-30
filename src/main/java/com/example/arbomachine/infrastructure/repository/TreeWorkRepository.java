package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.treework.entity.TreeWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Prace zawsze pobieramy w kontekscie drzewa. Wlasnosc projektu i przynaleznosc drzewa do projektu musza byc
 * sprawdzone WCZESNIEJ (FindTreeInProjectOwnedByCurrentUserHandler) - to repozytorium zaweza tylko do drzewa.
 */
@Repository
public interface TreeWorkRepository extends JpaRepository<TreeWork, Long> {

    /** Prace drzewa w kolejnosci dodawania. */
    List<TreeWork> findAllByTreeIdOrderByCreatedAtAscIdAsc(Long treeId);

    /** Pusty wynik takze dla pracy, ktora istnieje, ale nalezy do innego drzewa. */
    Optional<TreeWork> findByIdAndTreeId(Long treeWorkId, Long treeId);

    /**
     * Prace wszystkich drzew projektu JEDNYM zapytaniem (mapa projektu bez N+1), pogrupowane po drzewie
     * i w kolejnosci dodawania. Wlasnosc projektu sprawdza wczesniej handler.
     */
    List<TreeWork> findAllByTreeProjectIdOrderByTreeIdAscCreatedAtAscIdAsc(Long projectId);

    /** Liczba prac wszystkich drzew projektu - COUNT w bazie. */
    long countByTreeProjectId(Long projectId);
}

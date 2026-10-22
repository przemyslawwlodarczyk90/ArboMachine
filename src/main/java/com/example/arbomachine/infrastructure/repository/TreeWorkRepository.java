package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.treework.entity.TreeWorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    /** Etap 36: praca juz utworzona z tym identyfikatorem urzadzenia. */
    Optional<TreeWork> findByTreeIdAndClientRequestId(Long treeId, String clientRequestId);

    /**
     * Prace wszystkich drzew projektu JEDNYM zapytaniem (mapa projektu bez N+1), pogrupowane po drzewie
     * i w kolejnosci dodawania. Wlasnosc projektu sprawdza wczesniej handler.
     */
    List<TreeWork> findAllByTreeProjectIdOrderByTreeIdAscCreatedAtAscIdAsc(Long projectId);

    /** Liczba prac wszystkich drzew projektu - COUNT w bazie. */
    long countByTreeProjectId(Long projectId);

    /**
     * Prace o danym statusie i terminie we WSZYSTKICH projektach, z drzewem, projektem i wlascicielem (jedno
     * zapytanie) - tylko dla zadan systemowych (przypomnienia o pracach, etap 30), nigdy dla odpowiedzi API.
     */
    @Query("select work from TreeWork work join fetch work.tree tree join fetch tree.project project "
            + "join fetch project.owner owner where work.status = :status and work.plannedDate = :plannedDate "
            + "order by owner.id, project.id, tree.id, work.id")
    List<TreeWork> findAllWithOwnerByStatusAndPlannedDate(@Param("status") TreeWorkStatus status,
                                                          @Param("plannedDate") LocalDate plannedDate);
}

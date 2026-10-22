package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Wpisy monitoringu zawsze w kontekscie drzewa albo projektu. Wlasnosc projektu i przynaleznosc drzewa do projektu
 * musza byc sprawdzone WCZESNIEJ (handlery) - to repozytorium tylko zaweza.
 */
@Repository
public interface TreeMonitoringRecordRepository extends JpaRepository<TreeMonitoringRecord, Long> {

    /** Etap 36: kontrola juz utworzona z tym identyfikatorem urzadzenia. */
    Optional<TreeMonitoringRecord> findByTreeIdAndClientRequestId(Long treeId, String clientRequestId);


    /** Historia drzewa: najnowsza kontrola pierwsza (data, potem kolejnosc dodania). */
    List<TreeMonitoringRecord> findAllByTreeIdOrderByInspectionDateDescIdDesc(Long treeId);

    /** Pusty wynik takze dla wpisu, ktory istnieje, ale nalezy do innego drzewa. */
    Optional<TreeMonitoringRecord> findByIdAndTreeId(Long recordId, Long treeId);

    /** Wszystkie wpisy drzew projektu JEDNYM zapytaniem (eksport, dokument, ostatnia kontrola na liscie drzew). */
    @Query("select record from TreeMonitoringRecord record join fetch record.tree tree "
            + "where tree.project.id = :projectId order by tree.id asc, record.inspectionDate desc, record.id desc")
    List<TreeMonitoringRecord> findAllOfProjectNewestFirst(@Param("projectId") Long projectId);
}

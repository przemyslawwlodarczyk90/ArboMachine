package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.treemarking.entity.TreeMarking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Oznaczenia drzew zawsze w kontekscie drzewa / projektu - wlasnosc sprawdza wczesniej handler. */
@Repository
public interface TreeMarkingRepository extends JpaRepository<TreeMarking, Long> {

    List<TreeMarking> findAllByTreeId(Long treeId);

    /** Pary (id drzewa, kod) wszystkich oznaczen projektu jednym zapytaniem - mapa, eksport, lista. */
    @Query("select m.tree.id as treeId, m.code as code from TreeMarking m where m.tree.project.id = :projectId order by m.tree.id, m.id")
    List<TreeIdAndCode> findTreeIdsAndCodesByProjectId(@Param("projectId") Long projectId);

    interface TreeIdAndCode {
        Long getTreeId();

        String getCode();
    }
}

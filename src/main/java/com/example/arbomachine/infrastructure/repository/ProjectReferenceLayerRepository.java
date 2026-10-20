package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.referencelayer.entity.ProjectReferenceLayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Warstwy referencyjne zawsze w kontekscie projektu - wlasnosc projektu sprawdza wczesniej handler. */
@Repository
public interface ProjectReferenceLayerRepository extends JpaRepository<ProjectReferenceLayer, Long> {

    List<ProjectReferenceLayer> findAllByProjectIdOrderByCreatedAtAscIdAsc(Long projectId);

    /** Pusty wynik takze dla warstwy innego projektu. */
    Optional<ProjectReferenceLayer> findByIdAndProjectId(Long layerId, Long projectId);

    long countByProjectId(Long projectId);
}

package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Adnotacje zawsze w kontekscie zdjecia - wlasnosc projektu, drzewa i zdjecia sprawdza wczesniej
 * FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler. Nigdy wyszukiwanie po samym id adnotacji.
 */
@Repository
public interface TreePhotoAnnotationRepository extends JpaRepository<TreePhotoAnnotation, Long> {

    Optional<TreePhotoAnnotation> findByTreePhotoId(Long treePhotoId);

    /** Adnotacje wielu zdjec jednym zapytaniem (listy zdjec: hasAnnotation bez N+1). */
    List<TreePhotoAnnotation> findAllByTreePhotoIdIn(Collection<Long> treePhotoIds);
}

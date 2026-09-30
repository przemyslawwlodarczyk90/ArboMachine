package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Zdjecia zawsze pobieramy w kontekscie drzewa. Wlasnosc projektu i przynaleznosc drzewa do projektu musza byc
 * sprawdzone WCZESNIEJ (FindTreeInProjectOwnedByCurrentUserHandler) - to repozytorium zaweza tylko do drzewa.
 */
@Repository
public interface TreePhotoRepository extends JpaRepository<TreePhoto, Long> {

    /** Zdjecia drzewa w kolejnosci dodawania. */
    List<TreePhoto> findAllByTreeIdOrderByCreatedAtAscIdAsc(Long treeId);

    /** Pusty wynik takze dla zdjecia, ktore istnieje, ale nalezy do innego drzewa. */
    Optional<TreePhoto> findByIdAndTreeId(Long treePhotoId, Long treeId);

    /** Liczba zdjec (rekordow metadanych) wszystkich drzew projektu - COUNT w bazie, bez czytania plikow. */
    long countByTreeProjectId(Long projectId);
}

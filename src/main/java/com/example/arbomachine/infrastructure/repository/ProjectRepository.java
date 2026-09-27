package com.example.arbomachine.infrastructure.repository;

import com.example.arbomachine.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Projekty zawsze pobieramy razem z warunkiem na wlasciciela - nie ma tu metody "znajdz dowolny projekt po id"
 * dla kodu domenowego, wiec nie da sie przypadkiem odczytac cudzego projektu.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Projekty wlasciciela, najnowsze na poczatku. */
    List<Project> findAllByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /** Pusty wynik zarowno dla nieistniejacego projektu, jak i dla projektu innego uzytkownika. */
    Optional<Project> findByIdAndOwnerId(Long projectId, Long ownerId);

    boolean existsByOwnerId(Long ownerId);
}

package com.example.arbomachine.domain.project.service;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Odczyt projektow - ZAWSZE w kontekscie wlasciciela. To jedyne miejsce, przez ktore modul projektu
 * pobiera projekty, wiec kontrola wlasnosci nie moze zostac pominieta w pojedynczym endpoincie.
 */
@Service
public class ProjectQueryService {
    private final ProjectRepository projectRepository;

    public ProjectQueryService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public List<Project> findProjectsOwnedBy(Long ownerId) {
        return projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    /** Cudzy i nieistniejacy projekt daja ten sam wyjatek (-> 404). */
    public Project getProjectOwnedBy(Long projectId, Long ownerId) {
        return projectRepository.findByIdAndOwnerId(projectId, ownerId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));
    }
}

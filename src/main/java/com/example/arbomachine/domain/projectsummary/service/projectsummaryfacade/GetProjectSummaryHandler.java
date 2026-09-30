package com.example.arbomachine.domain.projectsummary.service.projectsummaryfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.projectsummary.dto.ProjectSummaryResponse;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Podsumowanie WLASNEGO projektu: najpierw wlasnosc (cudzy/nieistniejacy -> ProjectNotFoundException -> 404),
 * potem cztery zapytania COUNT w bazie - stala liczba zapytan niezaleznie od liczby drzew (brak N+1),
 * bez ladowania encji i bez czytania plikow zdjec. Liczba drzew bez lokalizacji jest roznica, wiec suma
 * zawsze zgadza sie z liczba drzew. Transakcja tylko do odczytu - wszystkie liczby z jednego widoku bazy.
 */
@Component
public class GetProjectSummaryHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeRepository treeRepository;
    private final TreePhotoRepository treePhotoRepository;
    private final TreeWorkRepository treeWorkRepository;

    public GetProjectSummaryHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                    TreeRepository treeRepository, TreePhotoRepository treePhotoRepository,
                                    TreeWorkRepository treeWorkRepository) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeRepository = treeRepository;
        this.treePhotoRepository = treePhotoRepository;
        this.treeWorkRepository = treeWorkRepository;
    }

    @Transactional(readOnly = true)
    public ProjectSummaryResponse handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        long treeCount = treeRepository.countByProjectId(ownedProject.getId());
        long treeWithLocationCount = treeRepository.countByProjectIdAndLocationLatitudeIsNotNull(ownedProject.getId());
        return new ProjectSummaryResponse(
                treeCount,
                treeWithLocationCount,
                treeCount - treeWithLocationCount,
                treePhotoRepository.countByTreeProjectId(ownedProject.getId()),
                treeWorkRepository.countByTreeProjectId(ownedProject.getId()));
    }
}

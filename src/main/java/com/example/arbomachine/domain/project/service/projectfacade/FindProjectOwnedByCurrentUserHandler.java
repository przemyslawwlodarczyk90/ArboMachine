package com.example.arbomachine.domain.project.service.projectfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import com.example.arbomachine.domain.user.service.useraccountfacade.FindUserAccountByEmailHandler;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import org.springframework.stereotype.Component;

/**
 * JEDYNE miejsce kontroli wlasnosci projektu: projekt o podanym id, ktory nalezy do zalogowanego uzytkownika.
 * Cudzy i nieistniejacy projekt daja ten sam wyjatek (-> 404). Wspolny handler: uzywany przez handlery
 * tej fasady oraz - jako waski kontrakt - przez modul drzew (wlasnosc drzewa = wlasnosc projektu).
 */
@Component
public class FindProjectOwnedByCurrentUserHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final ProjectRepository projectRepository;

    public FindProjectOwnedByCurrentUserHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                                ProjectRepository projectRepository) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.projectRepository = projectRepository;
    }

    public Project handle(String currentUserEmail, Long projectId) {
        Long currentUserAccountId = findUserAccountByEmailHandler.handle(currentUserEmail).getId();
        return projectRepository.findByIdAndOwnerId(projectId, currentUserAccountId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));
    }
}

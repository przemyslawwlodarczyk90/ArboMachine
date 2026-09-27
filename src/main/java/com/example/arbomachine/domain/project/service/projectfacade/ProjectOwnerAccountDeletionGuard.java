package com.example.arbomachine.domain.project.service.projectfacade;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.exception.UserAccountDeletionBlockedException;
import com.example.arbomachine.domain.user.service.useraccountfacade.UserAccountDeletionGuard;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import org.springframework.stereotype.Component;

/**
 * Tymczasowa polityka: konta z projektami nie wolno usunac, dopoki nie ustalimy archiwizacji
 * albo przenoszenia wlasnosci projektow. Bez tego baza odrzucilaby usuniecie kluczem obcym owner_id.
 */
@Component
public class ProjectOwnerAccountDeletionGuard implements UserAccountDeletionGuard {
    private final ProjectRepository projectRepository;

    public ProjectOwnerAccountDeletionGuard(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public void ensureUserAccountCanBeDeleted(UserAccount userAccountToDelete) {
        if (projectRepository.existsByOwnerId(userAccountToDelete.getId())) {
            throw new UserAccountDeletionBlockedException("User account owns projects and cannot be deleted");
        }
    }
}

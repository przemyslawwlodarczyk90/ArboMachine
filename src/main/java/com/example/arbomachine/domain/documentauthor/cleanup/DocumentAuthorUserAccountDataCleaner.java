package com.example.arbomachine.domain.documentauthor.cleanup;

import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.useraccountfacade.UserAccountDataCleaner;
import com.example.arbomachine.infrastructure.repository.DocumentAuthorProfileRepository;
import org.springframework.stereotype.Component;

/** Usuwane konto traci dane autora opracowania (wywolywane w transakcji usuwania konta). */
@Component
public class DocumentAuthorUserAccountDataCleaner implements UserAccountDataCleaner {
    private final DocumentAuthorProfileRepository documentAuthorProfileRepository;

    public DocumentAuthorUserAccountDataCleaner(DocumentAuthorProfileRepository documentAuthorProfileRepository) {
        this.documentAuthorProfileRepository = documentAuthorProfileRepository;
    }

    @Override
    public void deleteDataOf(UserAccount userAccountToDelete) {
        documentAuthorProfileRepository.findByUserAccountId(userAccountToDelete.getId())
                .ifPresent(documentAuthorProfileRepository::delete);
    }
}

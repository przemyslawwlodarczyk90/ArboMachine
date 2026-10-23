package com.example.arbomachine.domain.documentauthor.service.documentauthorprofilefacade;

import com.example.arbomachine.domain.documentauthor.dto.DocumentAuthorProfileResponse;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.useraccountfacade.FindUserAccountByEmailHandler;
import com.example.arbomachine.infrastructure.repository.DocumentAuthorProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Dane autora opracowania zalogowanego uzytkownika; brak -> pusty obiekt (hasProfile=false). */
@Component
public class GetCurrentUserDocumentAuthorProfileHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final DocumentAuthorProfileRepository documentAuthorProfileRepository;

    public GetCurrentUserDocumentAuthorProfileHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                                      DocumentAuthorProfileRepository documentAuthorProfileRepository) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.documentAuthorProfileRepository = documentAuthorProfileRepository;
    }

    @Transactional(readOnly = true)
    public DocumentAuthorProfileResponse handle(String currentUserEmail) {
        UserAccount currentUserAccount = findUserAccountByEmailHandler.handle(currentUserEmail);
        return documentAuthorProfileRepository.findByUserAccountId(currentUserAccount.getId())
                .map(DocumentAuthorProfileMapper::toResponse)
                .orElseGet(DocumentAuthorProfileResponse::empty);
    }
}

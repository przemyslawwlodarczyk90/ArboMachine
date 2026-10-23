package com.example.arbomachine.domain.documentauthor.facade;

import com.example.arbomachine.domain.documentauthor.dto.DocumentAuthorProfileRequest;
import com.example.arbomachine.domain.documentauthor.dto.DocumentAuthorProfileResponse;
import com.example.arbomachine.domain.documentauthor.service.documentauthorprofilefacade.GetCurrentUserDocumentAuthorProfileHandler;
import com.example.arbomachine.domain.documentauthor.service.documentauthorprofilefacade.SaveCurrentUserDocumentAuthorProfileHandler;
import org.springframework.stereotype.Component;

/**
 * Dane autora opracowania (etap 42) - stale elementy dokumentu projektu zapisane w profilu konta. Tylko deleguje
 * do handlerow.
 */
@Component
public class DocumentAuthorProfileFacade {
    private final GetCurrentUserDocumentAuthorProfileHandler getCurrentUserDocumentAuthorProfileHandler;
    private final SaveCurrentUserDocumentAuthorProfileHandler saveCurrentUserDocumentAuthorProfileHandler;

    public DocumentAuthorProfileFacade(GetCurrentUserDocumentAuthorProfileHandler getCurrentUserDocumentAuthorProfileHandler,
                                       SaveCurrentUserDocumentAuthorProfileHandler saveCurrentUserDocumentAuthorProfileHandler) {
        this.getCurrentUserDocumentAuthorProfileHandler = getCurrentUserDocumentAuthorProfileHandler;
        this.saveCurrentUserDocumentAuthorProfileHandler = saveCurrentUserDocumentAuthorProfileHandler;
    }

    public DocumentAuthorProfileResponse getProfileOfCurrentUser(String currentUserEmail) {
        return getCurrentUserDocumentAuthorProfileHandler.handle(currentUserEmail);
    }

    public DocumentAuthorProfileResponse saveProfileOfCurrentUser(String currentUserEmail, DocumentAuthorProfileRequest request) {
        return saveCurrentUserDocumentAuthorProfileHandler.handle(currentUserEmail, request);
    }
}

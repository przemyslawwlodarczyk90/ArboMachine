package com.example.arbomachine.domain.documentauthor.service.documentauthorprofilefacade;

import com.example.arbomachine.domain.documentauthor.entity.DocumentAuthorProfile;
import com.example.arbomachine.infrastructure.repository.DocumentAuthorProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Waski kontrakt dla modulu dokumentu projektu: dane autora wlasciciela projektu (wlasnosc projektu sprawdzona
 * wczesniej). Zwraca niezmienne Details, nie encje.
 */
@Component
public class FindDocumentAuthorDetailsOfUserAccountHandler {
    private final DocumentAuthorProfileRepository documentAuthorProfileRepository;

    public FindDocumentAuthorDetailsOfUserAccountHandler(DocumentAuthorProfileRepository documentAuthorProfileRepository) {
        this.documentAuthorProfileRepository = documentAuthorProfileRepository;
    }

    @Transactional(readOnly = true)
    public Optional<DocumentAuthorProfile.Details> handle(Long userAccountId) {
        return documentAuthorProfileRepository.findByUserAccountId(userAccountId).map(DocumentAuthorProfile::details);
    }
}

package com.example.arbomachine.domain.documentauthor.service.documentauthorprofilefacade;

import com.example.arbomachine.domain.documentauthor.dto.DocumentAuthorProfileRequest;
import com.example.arbomachine.domain.documentauthor.dto.DocumentAuthorProfileResponse;
import com.example.arbomachine.domain.documentauthor.entity.DocumentAuthorProfile;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.useraccountfacade.FindUserAccountByEmailHandler;
import com.example.arbomachine.infrastructure.repository.DocumentAuthorProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Zapis danych autora opracowania (PUT = utworz albo zastap calosc) dla zalogowanego uzytkownika. Wszystkie pola
 * puste -> dane usuniete (dokument pokaze "Dane do uzupelnienia"), bez osobnego DELETE.
 */
@Component
public class SaveCurrentUserDocumentAuthorProfileHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final DocumentAuthorProfileRepository documentAuthorProfileRepository;

    public SaveCurrentUserDocumentAuthorProfileHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                                       DocumentAuthorProfileRepository documentAuthorProfileRepository) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.documentAuthorProfileRepository = documentAuthorProfileRepository;
    }

    @Transactional
    public DocumentAuthorProfileResponse handle(String currentUserEmail, DocumentAuthorProfileRequest request) {
        UserAccount currentUserAccount = findUserAccountByEmailHandler.handle(currentUserEmail);
        DocumentAuthorProfile.Details details = DocumentAuthorProfileMapper.detailsOf(request);
        Optional<DocumentAuthorProfile> existingProfile = documentAuthorProfileRepository.findByUserAccountId(currentUserAccount.getId());
        if (details.isEmpty()) {
            existingProfile.ifPresent(documentAuthorProfileRepository::delete);
            return DocumentAuthorProfileResponse.empty();
        }
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        DocumentAuthorProfile profile = existingProfile
                .map(existing -> {
                    existing.replaceDetails(details, now);
                    return existing;
                })
                .orElseGet(() -> DocumentAuthorProfile.createFor(currentUserAccount, details, now));
        return DocumentAuthorProfileMapper.toResponse(documentAuthorProfileRepository.save(profile));
    }
}

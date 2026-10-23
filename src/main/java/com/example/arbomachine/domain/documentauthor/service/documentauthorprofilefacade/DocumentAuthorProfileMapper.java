package com.example.arbomachine.domain.documentauthor.service.documentauthorprofilefacade;

import com.example.arbomachine.domain.documentauthor.dto.DocumentAuthorProfileRequest;
import com.example.arbomachine.domain.documentauthor.dto.DocumentAuthorProfileResponse;
import com.example.arbomachine.domain.documentauthor.entity.DocumentAuthorProfile;

/** Request -> znormalizowane dane (przyciety tekst, puste = null) i encja -> odpowiedz API. Pomocnik handlerow fasady. */
final class DocumentAuthorProfileMapper {

    private DocumentAuthorProfileMapper() {
    }

    static DocumentAuthorProfile.Details detailsOf(DocumentAuthorProfileRequest request) {
        return new DocumentAuthorProfile.Details(trimmedOrNull(request.authorFullName()), trimmedOrNull(request.qualifications()),
                trimmedOrNull(request.companyName()), trimmedOrNull(request.companyDetails()),
                trimmedOrNull(request.methodsDescription()));
    }

    static DocumentAuthorProfileResponse toResponse(DocumentAuthorProfile profile) {
        return new DocumentAuthorProfileResponse(true, profile.getAuthorFullName(), profile.getQualifications(),
                profile.getCompanyName(), profile.getCompanyDetails(), profile.getMethodsDescription(), profile.getUpdatedAt());
    }

    private static String trimmedOrNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}

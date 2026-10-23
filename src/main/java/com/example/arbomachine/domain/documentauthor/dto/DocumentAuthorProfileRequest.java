package com.example.arbomachine.domain.documentauthor.dto;

import com.example.arbomachine.domain.documentauthor.entity.DocumentAuthorProfile;
import jakarta.validation.constraints.Size;

/**
 * Body PUT /api/users/me/document-author-profile - komplet danych (pominiete pole = brak wartosci). Wszystkie pola
 * puste = usuniecie danych (dokument wraca do "Dane do uzupelnienia").
 */
public record DocumentAuthorProfileRequest(
        @Size(max = DocumentAuthorProfile.MAXIMUM_SHORT_TEXT_LENGTH, message = "Author name must have at most 255 characters")
        String authorFullName,

        @Size(max = DocumentAuthorProfile.MAXIMUM_DETAILS_LENGTH, message = "Qualifications must have at most 2000 characters")
        String qualifications,

        @Size(max = DocumentAuthorProfile.MAXIMUM_SHORT_TEXT_LENGTH, message = "Company name must have at most 255 characters")
        String companyName,

        @Size(max = DocumentAuthorProfile.MAXIMUM_DETAILS_LENGTH, message = "Company details must have at most 2000 characters")
        String companyDetails,

        @Size(max = DocumentAuthorProfile.MAXIMUM_METHODS_DESCRIPTION_LENGTH, message = "Methods description must have at most 10000 characters")
        String methodsDescription
) {}

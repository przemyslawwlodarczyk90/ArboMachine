package com.example.arbomachine.domain.documentauthor.dto;

import java.time.Instant;

/**
 * Dane autora opracowania w API. hasProfile = false -> konto nie ma jeszcze danych (pozostale pola null) - GET
 * zwraca wtedy 200 z pustym obiektem zamiast 404 (jak waloryzacja drzewa).
 */
public record DocumentAuthorProfileResponse(
        boolean hasProfile,
        String authorFullName,
        String qualifications,
        String companyName,
        String companyDetails,
        String methodsDescription,
        Instant updatedAt
) {

    public static DocumentAuthorProfileResponse empty() {
        return new DocumentAuthorProfileResponse(false, null, null, null, null, null, null);
    }
}

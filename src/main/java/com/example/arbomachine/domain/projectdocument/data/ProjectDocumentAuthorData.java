package com.example.arbomachine.domain.projectdocument.data;

/**
 * Etap 42: dane autora opracowania i wykonawcy z profilu wlasciciela projektu (kazde pole moze byc null).
 * Brak calego obiektu w ProjectDocumentData = autor nie uzupelnil danych -> "Dane do uzupelnienia".
 */
public record ProjectDocumentAuthorData(
        String authorFullName,
        String qualifications,
        String companyName,
        String companyDetails,
        String methodsDescription
) {}

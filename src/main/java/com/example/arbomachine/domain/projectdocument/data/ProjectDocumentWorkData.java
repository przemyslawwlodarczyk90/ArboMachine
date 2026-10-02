package com.example.arbomachine.domain.projectdocument.data;

import java.time.LocalDate;

/**
 * Zalecenie / praca przy drzewie: nazwa i opis wpisane przez arboryste oraz (etap 28) status, priorytet
 * organizacyjny (kody albo null) i terminy (null = nie podano).
 */
public record ProjectDocumentWorkData(String name, String description, String status, String priority,
                                      LocalDate plannedDate, LocalDate completedDate) {

    public boolean isOpen() {
        return "RECOMMENDED".equals(status) || "PLANNED".equals(status);
    }
}

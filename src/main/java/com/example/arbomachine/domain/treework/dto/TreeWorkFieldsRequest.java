package com.example.arbomachine.domain.treework.dto;

/**
 * Pola pracy wspolne dla CreateTreeWorkRequest i UpdateTreeWorkRequest - pozwala znormalizowac oba jednym kodem.
 * Oba requesty pozostaja osobnymi operacjami (inne endpointy).
 */
public interface TreeWorkFieldsRequest {

    String name();

    String description();

    String status();

    String priority();

    java.time.LocalDate plannedDate();

    java.time.LocalDate completedDate();
}

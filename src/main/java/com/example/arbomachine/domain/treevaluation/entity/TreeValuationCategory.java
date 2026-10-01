package com.example.arbomachine.domain.treevaluation.entity;

/**
 * Ogolna kategoria waloryzacji V1 - TO NIE JEST POTWIERDZONA SKALA BRANZOWA. Tymczasowa klasyfikacja do filtrowania,
 * kolorowania mapy, dokumentu i rozmowy z arborysta (docs/DO_DOPRECYZOWANIA.md). Zawsze wybierana recznie przez
 * arboryste - system jej nie wylicza. Brak kategorii (null) = "Nie oceniono". Zapis jako tekst (varchar), nie enum
 * bazy - skala moze sie zmienic po discovery.
 */
public enum TreeValuationCategory {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

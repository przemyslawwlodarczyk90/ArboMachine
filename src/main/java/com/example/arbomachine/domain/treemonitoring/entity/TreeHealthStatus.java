package com.example.arbomachine.domain.treemonitoring.entity;

/**
 * Ocena stanu drzewa przy kontroli V1 (etap 29) - TO NIE JEST POTWIERDZONA SKALA BRANZOWA (nie VTA, nie klasa
 * ryzyka). Tymczasowa, reczna ocena arborysty do historii, filtrowania, eksportu i jednej reguly alertu (POOR).
 * Brak oceny (null) = "Nie okreslono". Zapis jako tekst - skala moze sie zmienic po discovery (docs/DO_DOPRECYZOWANIA.md).
 */
public enum TreeHealthStatus {
    /** stan dobry */
    GOOD,
    /** stan umiarkowany */
    FAIR,
    /** stan zly - wymaga uwagi (regula alertu V1) */
    POOR
}

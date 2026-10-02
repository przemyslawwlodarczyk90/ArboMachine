package com.example.arbomachine.domain.treework.entity;

/**
 * Status pracy w gospodarce drzewostanem V1 (etap 28). Bez maszyny stanow - dowolna zmiana jest dozwolona
 * (reguly przejsc nie sa potwierdzone). Zapis jako tekst; null w bazie (prace sprzed etapu 28) = RECOMMENDED.
 */
public enum TreeWorkStatus {
    /** arborysta zaleca wykonanie pracy */
    RECOMMENDED,
    /** praca przyjeta do planu gospodarki */
    PLANNED,
    /** praca wykonana */
    COMPLETED,
    /** zrezygnowano z realizacji */
    CANCELLED;

    /** Prace "do wykonania" (zalecone albo zaplanowane) - widok gospodarki, mapa, eksport GIS. */
    public boolean isOpen() {
        return this == RECOMMENDED || this == PLANNED;
    }
}

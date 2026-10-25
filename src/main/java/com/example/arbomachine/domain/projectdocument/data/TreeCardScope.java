package com.example.arbomachine.domain.projectdocument.data;

/**
 * Etap 54 (02 poz. 8.4): ktore drzewa dostaja w dokumencie PDF pelna karte (dane, waloryzacja, prace, zdjecia).
 * Zestawienie drzew i gospodarka drzewostanem zawsze obejmuja wszystkie drzewa. Duzy projekt (938 drzew = ok. 530
 * stron) mozna skrocic bez usuwania danych.
 */
public enum TreeCardScope {
    /** wszystkie drzewa (jak przed etapem 54) */
    ALL,
    /** tylko drzewa z praca zalecona albo zaplanowana */
    WITH_OPEN_WORKS,
    /** bez kart drzew - sam opis projektu, zestawienia i mapa */
    NONE;

    /** Czy drzewo z tymi pracami dostaje karte. */
    public boolean includesCardOf(java.util.List<ProjectDocumentWorkData> treeWorks) {
        return switch (this) {
            case ALL -> true;
            case WITH_OPEN_WORKS -> treeWorks.stream().anyMatch(ProjectDocumentWorkData::isOpen);
            case NONE -> false;
        };
    }
}

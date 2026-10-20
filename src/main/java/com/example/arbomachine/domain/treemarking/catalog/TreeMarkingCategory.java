package com.example.arbomachine.domain.treemarking.catalog;

/** Grupa oznaczen drzewa (etap 31) - tylko do porzadku w UI i legendzie; kolejnosc = kolejnosc wyswietlania. */
public enum TreeMarkingCategory {
    VALUE("Wartość"),
    MANAGEMENT("Postępowanie"),
    CONDITION("Stan"),
    HAZARD("Zagrożenie"),
    HABITAT("Siedlisko i przyroda");

    private final String displayName;

    TreeMarkingCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

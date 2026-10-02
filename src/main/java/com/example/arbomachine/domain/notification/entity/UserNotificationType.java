package com.example.arbomachine.domain.notification.entity;

/** Rodzaj powiadomienia V1 - kazdy ma jedna regule zrodlowa (DO_DOPRECYZOWANIA: jakie alerty sa potrzebne). */
public enum UserNotificationType {
    /** etap 29: kontrola drzewa z ocena "zly" (POOR) */
    TREE_POOR_HEALTH,
    /** etap 30: prace zaplanowane (PLANNED) na jutro - jedno zbiorcze przypomnienie dziennie */
    WORKS_DUE_TOMORROW,
    /** przycisk "Wyslij powiadomienie testowe" */
    TEST
}

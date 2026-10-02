package com.example.arbomachine.domain.treework.entity;

/**
 * Priorytet ORGANIZACYJNY V1 (etap 28) - to NIE jest standard arborystyczny ani ocena ryzyka. Opcjonalny (null = nie
 * ustawiono), ustawiany recznie; nigdy wyliczany (np. z waloryzacji). Do uzgodnienia (docs/DO_DOPRECYZOWANIA.md).
 */
public enum TreeWorkPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

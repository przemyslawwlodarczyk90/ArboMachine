package com.example.arbomachine.domain.user.enums;

/**
 * Role, ktore mozna nadac przez API: wybrac przy rejestracji albo ustawic z panelu admina.
 * Celowo BEZ ADMIN - dzieki temu nikt nie nada sobie ani innym uprawnien administratora przez aplikacje.
 */
public enum AssignableUserRole {
    ARBORIST,
    OFFICIAL;

    public UserRole toUserRole() {
        return UserRole.valueOf(name());
    }
}

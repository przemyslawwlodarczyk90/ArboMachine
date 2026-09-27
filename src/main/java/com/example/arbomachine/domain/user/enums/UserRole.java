package com.example.arbomachine.domain.user.enums;

/**
 * Rola konta w systemie - decyduje, do ktorego panelu uzytkownik trafia po zalogowaniu
 * i do ktorych endpointow ma dostep.
 * <p>
 * ARBORIST i OFFICIAL uzytkownik wybiera sam przy rejestracji. ADMIN nadaje sie wylacznie recznie w bazie:
 * {@code UPDATE arbomachine.user_account SET role = 'ADMIN' WHERE email = '...';}
 */
public enum UserRole {
    ARBORIST,
    OFFICIAL,
    ADMIN;

    /** Nazwa uprawnienia w Spring Security (np. "ROLE_ADMIN") - sprawdzana przez hasRole("ADMIN"). */
    public String toSpringSecurityAuthority() {
        return "ROLE_" + name();
    }
}

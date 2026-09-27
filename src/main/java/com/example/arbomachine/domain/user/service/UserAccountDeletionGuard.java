package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.entity.UserAccount;

/**
 * Punkt rozszerzenia dla innych modulow: pozwala zablokowac usuniecie konta, do ktorego naleza ich dane.
 * Modul uzytkownikow nie zna tych modulow - dostaje po prostu liste wszystkich beanow tego typu.
 */
public interface UserAccountDeletionGuard {

    /** Rzuca UserAccountDeletionBlockedException, jesli konta nie wolno usunac. */
    void ensureUserAccountCanBeDeleted(UserAccount userAccountToDelete);
}

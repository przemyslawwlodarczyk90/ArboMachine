package com.example.arbomachine.domain.user.service.useraccountfacade;

import com.example.arbomachine.domain.user.entity.UserAccount;

/**
 * Punkt rozszerzenia dla innych modulow: usuniecie ich danych pomocniczych konta (np. powiadomien, subskrypcji
 * push) tuz przed usunieciem konta - po sprawdzeniu wszystkich UserAccountDeletionGuard. Dane biznesowe (projekty)
 * blokuja usuniecie przez guard, a nie sa tu kasowane.
 */
public interface UserAccountDataCleaner {

    void deleteDataOf(UserAccount userAccountToDelete);
}

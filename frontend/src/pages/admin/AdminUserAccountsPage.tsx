import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { adminUserAccountsApi } from './adminUserAccountsApi';
import type { AssignableUserRole, UserAccount, UserRole } from '../../api/apiDataTypes';
import { useAuthentication } from '../../auth/AuthenticationContext';
import { USER_ROLE_DISPLAY_NAMES } from '../../auth/userRoles';
import { Alert, type AlertVariant } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { ModalDialog } from '../../components/ModalDialog';
import { UserProfileForm } from '../../components/UserProfileForm';

type AccountStatus = 'active' | 'unconfirmed' | 'locked';
type AccountStatusFilter = 'all' | AccountStatus;

const ROLE_FILTER_OPTIONS: { role: UserRole | ''; label: string }[] = [
  { role: '', label: 'Wszystkie role' },
  { role: 'ARBORIST', label: 'Arboryści' },
  { role: 'OFFICIAL', label: 'Urzędnicy' },
  { role: 'ADMIN', label: 'Administratorzy' },
];

const ACCOUNT_STATUS_LABELS: Record<AccountStatus, string> = {
  active: 'Aktywne',
  unconfirmed: 'Niepotwierdzone',
  locked: 'Zablokowane',
};

/** Blokada admina ma pierwszenstwo przed brakiem potwierdzenia e-maila. */
function determineAccountStatus(userAccount: UserAccount): AccountStatus {
  if (userAccount.lockedByAdmin) return 'locked';
  if (!userAccount.emailConfirmed) return 'unconfirmed';
  return 'active';
}

const fullNameOf = (userAccount: UserAccount) => `${userAccount.firstName} ${userAccount.lastName}`;

/**
 * Lista kont z akcjami admina. Filtry roli i statusu siedza w adresie (?role=...&status=...),
 * wiec kafelki z pulpitu moga linkowac prosto do przefiltrowanej listy.
 */
export function AdminUserAccountsPage() {
  const { currentUserAccount } = useAuthentication();
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedRoleFilter = (searchParams.get('role') ?? '') as UserRole | '';
  const selectedStatusFilter = (searchParams.get('status') ?? 'all') as AccountStatusFilter;

  const [loadedUserAccounts, setLoadedUserAccounts] = useState<UserAccount[] | null>(null);
  const [loadingErrorMessage, setLoadingErrorMessage] = useState<string | null>(null);
  const [actionResultMessage, setActionResultMessage] = useState<{ variant: AlertVariant; text: string } | null>(null);
  const [searchText, setSearchText] = useState('');
  const [accountIdWithActionInProgress, setAccountIdWithActionInProgress] = useState<number | null>(null);
  const [accountBeingEdited, setAccountBeingEdited] = useState<UserAccount | null>(null);
  const [accountIdAwaitingDeletionConfirmation, setAccountIdAwaitingDeletionConfirmation] = useState<number | null>(null);

  const loadUserAccounts = useCallback(() => {
    setLoadingErrorMessage(null);
    setLoadedUserAccounts(null);
    adminUserAccountsApi.listUserAccounts(selectedRoleFilter || undefined)
      .then(setLoadedUserAccounts)
      .catch((requestError) =>
        setLoadingErrorMessage(requestError instanceof ApiRequestError ? requestError.message : 'Nieoczekiwany błąd.'));
  }, [selectedRoleFilter]);

  useEffect(loadUserAccounts, [loadUserAccounts]);

  const changeFilterInUrl = (filterName: 'role' | 'status', filterValue: string) => {
    const updatedSearchParams = new URLSearchParams(searchParams);
    if (filterValue && filterValue !== 'all') updatedSearchParams.set(filterName, filterValue);
    else updatedSearchParams.delete(filterName);
    setSearchParams(updatedSearchParams, { replace: true });
  };

  const visibleUserAccounts = useMemo(() => {
    if (!loadedUserAccounts) return null;
    const normalizedSearchText = searchText.trim().toLowerCase();
    return loadedUserAccounts.filter((userAccount) =>
      (selectedStatusFilter === 'all' || determineAccountStatus(userAccount) === selectedStatusFilter) &&
      (!normalizedSearchText || [userAccount.firstName, userAccount.lastName, userAccount.email, userAccount.city]
        .some((searchedValue) => searchedValue?.toLowerCase().includes(normalizedSearchText))),
    );
  }, [loadedUserAccounts, searchText, selectedStatusFilter]);

  const replaceAccountInList = (updatedAccount: UserAccount) =>
    setLoadedUserAccounts((currentList) =>
      currentList?.map((listedAccount) => (listedAccount.id === updatedAccount.id ? updatedAccount : listedAccount)) ?? null);

  const removeAccountFromList = (removedAccountId: number) =>
    setLoadedUserAccounts((currentList) => currentList?.filter((listedAccount) => listedAccount.id !== removedAccountId) ?? null);

  /**
   * Wykonuje akcje admina na jednym wierszu: blokuje jego przyciski na czas zadania, aktualizuje tabele
   * (akcja zwracajaca konto -> podmiana, bez wyniku -> usuniecie z listy) i pokazuje komunikat.
   */
  const runAdminActionOnAccount = async (
    targetAccount: UserAccount,
    adminAction: () => Promise<UserAccount | void>,
    successMessage: string,
  ) => {
    setActionResultMessage(null);
    setAccountIdWithActionInProgress(targetAccount.id);
    try {
      const updatedAccount = await adminAction();
      if (updatedAccount) replaceAccountInList(updatedAccount);
      else removeAccountFromList(targetAccount.id);
      setActionResultMessage({ variant: 'success', text: successMessage });
    } catch (actionError) {
      setActionResultMessage({ variant: 'error', text: actionError instanceof ApiRequestError ? actionError.message : 'Nieoczekiwany błąd.' });
    } finally {
      setAccountIdWithActionInProgress(null);
      setAccountIdAwaitingDeletionConfirmation(null);
    }
  };

  return (
    <div className="stack-lg">
      <header className="page-header page-header-row">
        <div>
          <h1>Użytkownicy</h1>
          <p className="muted">Zarządzanie kontami arborystów i urzędników.</p>
        </div>
        <button className="btn btn-secondary" onClick={loadUserAccounts}>Odśwież</button>
      </header>

      {actionResultMessage && <Alert variant={actionResultMessage.variant}>{actionResultMessage.text}</Alert>}

      <section className="card card-flush">
        <div className="table-toolbar">
          <div className="segmented" role="group" aria-label="Filtr roli">
            {ROLE_FILTER_OPTIONS.map((roleFilterOption) => (
              <button key={roleFilterOption.role}
                      className={selectedRoleFilter === roleFilterOption.role ? 'active' : undefined}
                      onClick={() => changeFilterInUrl('role', roleFilterOption.role)}>
                {roleFilterOption.label}
              </button>
            ))}
          </div>
          <select className="select" value={selectedStatusFilter} aria-label="Filtr statusu"
                  onChange={(changeEvent) => changeFilterInUrl('status', changeEvent.target.value)}>
            <option value="all">Dowolny status</option>
            <option value="active">Aktywne</option>
            <option value="unconfirmed">Niepotwierdzone</option>
            <option value="locked">Zablokowane</option>
          </select>
          <input className="search" type="search" placeholder="Szukaj: imię, e-mail, miejscowość…" aria-label="Szukaj użytkowników"
                 value={searchText} onChange={(changeEvent) => setSearchText(changeEvent.target.value)} />
          {loadedUserAccounts && visibleUserAccounts && (
            <span className="muted small toolbar-count">{visibleUserAccounts.length} z {loadedUserAccounts.length}</span>
          )}
        </div>

        {loadingErrorMessage && <div className="pad"><Alert variant="error">{loadingErrorMessage}</Alert></div>}
        {!loadedUserAccounts && !loadingErrorMessage && <div className="pad center"><LoadingSpinner /></div>}

        {visibleUserAccounts && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Imię i nazwisko</th>
                  <th>E-mail</th>
                  <th>Rola</th>
                  <th>Status</th>
                  <th className="right">Akcje</th>
                </tr>
              </thead>
              <tbody>
                {visibleUserAccounts.map((userAccount) => {
                  const isAdminAccount = userAccount.role === 'ADMIN';
                  const isActionInProgress = accountIdWithActionInProgress === userAccount.id;
                  const isCurrentUser = userAccount.id === currentUserAccount?.id;
                  const accountStatus = determineAccountStatus(userAccount);
                  return (
                    <tr key={userAccount.id} className={isCurrentUser ? 'row-me' : undefined}>
                      <td className="num">{userAccount.id}</td>
                      <td>{fullNameOf(userAccount)} {isCurrentUser && <span className="badge">Ty</span>}</td>
                      <td>{userAccount.email}</td>
                      <td>
                        {isAdminAccount ? (
                          <span className="role-pill role-admin">Administrator</span>
                        ) : (
                          <select className="select select-sm" value={userAccount.role} disabled={isActionInProgress}
                                  aria-label={`Rola: ${fullNameOf(userAccount)}`}
                                  onChange={(changeEvent) => {
                                    const newRole = changeEvent.target.value as AssignableUserRole;
                                    runAdminActionOnAccount(userAccount,
                                      () => adminUserAccountsApi.changeUserRole(userAccount.id, newRole),
                                      `${fullNameOf(userAccount)} ma teraz rolę: ${USER_ROLE_DISPLAY_NAMES[newRole].toLowerCase()}.`);
                                  }}>
                            <option value="ARBORIST">Arborysta</option>
                            <option value="OFFICIAL">Urzędnik</option>
                          </select>
                        )}
                      </td>
                      <td><span className={`status status-${accountStatus}`}>{ACCOUNT_STATUS_LABELS[accountStatus]}</span></td>
                      <td className="right">
                        {isAdminAccount ? (
                          <span className="muted small">Zarządzane w bazie danych</span>
                        ) : accountIdAwaitingDeletionConfirmation === userAccount.id ? (
                          <div className="row-actions">
                            <span className="small">Usunąć: {userAccount.firstName}?</span>
                            <button className="btn btn-sm btn-ghost" disabled={isActionInProgress}
                                    onClick={() => setAccountIdAwaitingDeletionConfirmation(null)}>
                              Nie
                            </button>
                            <button className="btn btn-sm btn-danger" disabled={isActionInProgress}
                                    onClick={() => runAdminActionOnAccount(userAccount,
                                      () => adminUserAccountsApi.deleteUserAccount(userAccount.id),
                                      `Konto ${fullNameOf(userAccount)} zostało usunięte.`)}>
                              Tak, usuń
                            </button>
                          </div>
                        ) : (
                          <div className="row-actions">
                            <button className="btn btn-sm btn-secondary" disabled={isActionInProgress}
                                    onClick={() => setAccountBeingEdited(userAccount)}>
                              Edytuj
                            </button>
                            {!userAccount.emailConfirmed && (
                              <button className="btn btn-sm btn-secondary" disabled={isActionInProgress}
                                      onClick={() => runAdminActionOnAccount(userAccount,
                                        () => adminUserAccountsApi.confirmEmailOnUsersBehalf(userAccount.id),
                                        `E-mail użytkownika ${fullNameOf(userAccount)} został potwierdzony.`)}>
                                Potwierdź e-mail
                              </button>
                            )}
                            <button className="btn btn-sm btn-secondary" disabled={isActionInProgress}
                                    onClick={() => runAdminActionOnAccount(userAccount,
                                      () => adminUserAccountsApi.setAccountLockedByAdmin(userAccount.id, !userAccount.lockedByAdmin),
                                      `Konto ${fullNameOf(userAccount)} zostało ${userAccount.lockedByAdmin ? 'odblokowane' : 'zablokowane'}.`)}>
                              {userAccount.lockedByAdmin ? 'Odblokuj' : 'Zablokuj'}
                            </button>
                            <button className="btn btn-sm btn-danger-outline" disabled={isActionInProgress}
                                    onClick={() => setAccountIdAwaitingDeletionConfirmation(userAccount.id)}>
                              Usuń
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })}
                {visibleUserAccounts.length === 0 && (
                  <tr><td colSpan={6} className="center muted">Brak użytkowników pasujących do filtrów.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {accountBeingEdited && (
        <ModalDialog title={`Edycja: ${fullNameOf(accountBeingEdited)}`} closeDialog={() => setAccountBeingEdited(null)}>
          <p className="muted small">{USER_ROLE_DISPLAY_NAMES[accountBeingEdited.role]} · {accountBeingEdited.email}</p>
          <UserProfileForm
            editedAccount={accountBeingEdited}
            closeForm={() => setAccountBeingEdited(null)}
            saveProfile={async (updatedProfile) => {
              const savedAccount = await adminUserAccountsApi.updateUserProfile(accountBeingEdited.id, updatedProfile);
              replaceAccountInList(savedAccount);
              setAccountBeingEdited(savedAccount);
              return savedAccount;
            }}
          />
        </ModalDialog>
      )}
    </div>
  );
}

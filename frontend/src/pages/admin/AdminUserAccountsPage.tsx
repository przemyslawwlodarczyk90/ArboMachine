import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ApiRequestError, arboMachineApi } from '../../api/arboMachineApiClient';
import type { AssignableUserRole, UserAccount, UserRole } from '../../api/apiDataTypes';
import { useAuthentication } from '../../auth/AuthenticationContext';
import { USER_ROLE_DISPLAY_NAMES } from '../../auth/userRoles';
import { Alert, type AlertVariant } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { UserProfileForm } from '../../components/UserProfileForm';

type AccountStatus = 'active' | 'unconfirmed' | 'locked';
type AccountStatusFilter = 'all' | AccountStatus;

const ROLE_FILTER_OPTIONS: { role: UserRole | ''; label: string }[] = [
  { role: '', label: 'All roles' },
  { role: 'ARBORIST', label: 'Arborists' },
  { role: 'OFFICIAL', label: 'Officials' },
  { role: 'ADMIN', label: 'Administrators' },
];

const ACCOUNT_STATUS_LABELS: Record<AccountStatus, string> = {
  active: 'Active',
  unconfirmed: 'Unconfirmed',
  locked: 'Locked',
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
    arboMachineApi.admin.listUserAccounts(selectedRoleFilter || undefined)
      .then(setLoadedUserAccounts)
      .catch((requestError) =>
        setLoadingErrorMessage(requestError instanceof ApiRequestError ? requestError.message : 'Unexpected error.'));
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
      setActionResultMessage({ variant: 'error', text: actionError instanceof ApiRequestError ? actionError.message : 'Unexpected error.' });
    } finally {
      setAccountIdWithActionInProgress(null);
      setAccountIdAwaitingDeletionConfirmation(null);
    }
  };

  return (
    <div className="stack-lg">
      <header className="page-header page-header-row">
        <div>
          <h1>Users</h1>
          <p className="muted">Manage arborist and official accounts.</p>
        </div>
        <button className="btn btn-secondary" onClick={loadUserAccounts}>Refresh</button>
      </header>

      {actionResultMessage && <Alert variant={actionResultMessage.variant}>{actionResultMessage.text}</Alert>}

      <section className="card card-flush">
        <div className="table-toolbar">
          <div className="segmented" role="group" aria-label="Filter by role">
            {ROLE_FILTER_OPTIONS.map((roleFilterOption) => (
              <button key={roleFilterOption.role}
                      className={selectedRoleFilter === roleFilterOption.role ? 'active' : undefined}
                      onClick={() => changeFilterInUrl('role', roleFilterOption.role)}>
                {roleFilterOption.label}
              </button>
            ))}
          </div>
          <select className="select" value={selectedStatusFilter} aria-label="Filter by status"
                  onChange={(changeEvent) => changeFilterInUrl('status', changeEvent.target.value)}>
            <option value="all">Any status</option>
            <option value="active">Active</option>
            <option value="unconfirmed">Unconfirmed</option>
            <option value="locked">Locked</option>
          </select>
          <input className="search" type="search" placeholder="Search name, e-mail, city…" aria-label="Search users"
                 value={searchText} onChange={(changeEvent) => setSearchText(changeEvent.target.value)} />
          {loadedUserAccounts && visibleUserAccounts && (
            <span className="muted small toolbar-count">{visibleUserAccounts.length} of {loadedUserAccounts.length}</span>
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
                  <th>Name</th>
                  <th>E-mail</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th className="right">Actions</th>
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
                      <td>{fullNameOf(userAccount)} {isCurrentUser && <span className="badge">You</span>}</td>
                      <td>{userAccount.email}</td>
                      <td>
                        {isAdminAccount ? (
                          <span className="role-pill role-admin">Administrator</span>
                        ) : (
                          <select className="select select-sm" value={userAccount.role} disabled={isActionInProgress}
                                  aria-label={`Role of ${fullNameOf(userAccount)}`}
                                  onChange={(changeEvent) => {
                                    const newRole = changeEvent.target.value as AssignableUserRole;
                                    runAdminActionOnAccount(userAccount,
                                      () => arboMachineApi.admin.changeUserRole(userAccount.id, newRole),
                                      `${fullNameOf(userAccount)} is now ${USER_ROLE_DISPLAY_NAMES[newRole].toLowerCase()}.`);
                                  }}>
                            <option value="ARBORIST">Arborist</option>
                            <option value="OFFICIAL">Official</option>
                          </select>
                        )}
                      </td>
                      <td><span className={`status status-${accountStatus}`}>{ACCOUNT_STATUS_LABELS[accountStatus]}</span></td>
                      <td className="right">
                        {isAdminAccount ? (
                          <span className="muted small">Managed in database</span>
                        ) : accountIdAwaitingDeletionConfirmation === userAccount.id ? (
                          <div className="row-actions">
                            <span className="small">Delete {userAccount.firstName}?</span>
                            <button className="btn btn-sm btn-ghost" disabled={isActionInProgress}
                                    onClick={() => setAccountIdAwaitingDeletionConfirmation(null)}>
                              No
                            </button>
                            <button className="btn btn-sm btn-danger" disabled={isActionInProgress}
                                    onClick={() => runAdminActionOnAccount(userAccount,
                                      () => arboMachineApi.admin.deleteUserAccount(userAccount.id),
                                      `${fullNameOf(userAccount)} has been deleted.`)}>
                              Yes, delete
                            </button>
                          </div>
                        ) : (
                          <div className="row-actions">
                            <button className="btn btn-sm btn-secondary" disabled={isActionInProgress}
                                    onClick={() => setAccountBeingEdited(userAccount)}>
                              Edit
                            </button>
                            {!userAccount.emailConfirmed && (
                              <button className="btn btn-sm btn-secondary" disabled={isActionInProgress}
                                      onClick={() => runAdminActionOnAccount(userAccount,
                                        () => arboMachineApi.admin.confirmEmailOnUsersBehalf(userAccount.id),
                                        `${fullNameOf(userAccount)}'s e-mail has been confirmed.`)}>
                                Confirm e-mail
                              </button>
                            )}
                            <button className="btn btn-sm btn-secondary" disabled={isActionInProgress}
                                    onClick={() => runAdminActionOnAccount(userAccount,
                                      () => arboMachineApi.admin.setAccountLockedByAdmin(userAccount.id, !userAccount.lockedByAdmin),
                                      `${fullNameOf(userAccount)} has been ${userAccount.lockedByAdmin ? 'unlocked' : 'locked'}.`)}>
                              {userAccount.lockedByAdmin ? 'Unlock' : 'Lock'}
                            </button>
                            <button className="btn btn-sm btn-danger-outline" disabled={isActionInProgress}
                                    onClick={() => setAccountIdAwaitingDeletionConfirmation(userAccount.id)}>
                              Delete
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  );
                })}
                {visibleUserAccounts.length === 0 && (
                  <tr><td colSpan={6} className="center muted">No users match the filters.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {accountBeingEdited && (
        <div className="modal-backdrop" onClick={() => setAccountBeingEdited(null)}>
          <div className="card modal" role="dialog" aria-modal="true" aria-labelledby="edit-account-dialog-title"
               onClick={(clickEvent) => clickEvent.stopPropagation()}>
            <h2 id="edit-account-dialog-title">Edit {fullNameOf(accountBeingEdited)}</h2>
            <p className="muted small">{USER_ROLE_DISPLAY_NAMES[accountBeingEdited.role]} · {accountBeingEdited.email}</p>
            <UserProfileForm
              editedAccount={accountBeingEdited}
              closeForm={() => setAccountBeingEdited(null)}
              saveProfile={async (updatedProfile) => {
                const savedAccount = await arboMachineApi.admin.updateUserProfile(accountBeingEdited.id, updatedProfile);
                replaceAccountInList(savedAccount);
                setAccountBeingEdited(savedAccount);
                return savedAccount;
              }}
            />
          </div>
        </div>
      )}
    </div>
  );
}

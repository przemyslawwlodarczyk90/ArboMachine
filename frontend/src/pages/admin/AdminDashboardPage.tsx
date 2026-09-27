import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError, arboMachineApi } from '../../api/arboMachineApiClient';
import type { RolePanelWelcome, UserAccountStatistics } from '../../api/apiDataTypes';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';

/** Pulpit admina: kafelki z liczbami kont (klikniecie otwiera przefiltrowana liste) i instrukcja nadawania admina. */
export function AdminDashboardPage() {
  const [panelWelcome, setPanelWelcome] = useState<RolePanelWelcome | null>(null);
  const [accountStatistics, setAccountStatistics] = useState<UserAccountStatistics | null>(null);
  const [loadingErrorMessage, setLoadingErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([arboMachineApi.getRolePanelWelcome('ADMIN'), arboMachineApi.admin.getUserAccountStatistics()])
      .then(([loadedPanelWelcome, loadedStatistics]) => {
        setPanelWelcome(loadedPanelWelcome);
        setAccountStatistics(loadedStatistics);
      })
      .catch((requestError) =>
        setLoadingErrorMessage(requestError instanceof ApiRequestError ? requestError.message : 'Unexpected error.'));
  }, []);

  const statisticTiles = accountStatistics && [
    { label: 'All accounts', count: accountStatistics.totalAccounts, userListPath: '/admin/users' },
    { label: 'Arborists', count: accountStatistics.arboristAccounts, userListPath: '/admin/users?role=ARBORIST' },
    { label: 'Officials', count: accountStatistics.officialAccounts, userListPath: '/admin/users?role=OFFICIAL' },
    { label: 'Administrators', count: accountStatistics.adminAccounts, userListPath: '/admin/users?role=ADMIN' },
    { label: 'Awaiting e-mail confirmation', count: accountStatistics.accountsAwaitingEmailConfirmation, userListPath: '/admin/users?status=unconfirmed' },
    { label: 'Locked', count: accountStatistics.accountsLockedByAdmin, userListPath: '/admin/users?status=locked' },
  ];

  return (
    <div className="stack-lg">
      <header className="page-header">
        <h1>{panelWelcome ? `Hello, ${panelWelcome.firstName}` : 'Dashboard'}</h1>
        <p className="muted">{panelWelcome?.welcomeMessage ?? 'Administration overview.'}</p>
      </header>

      {loadingErrorMessage && <Alert variant="error">{loadingErrorMessage}</Alert>}
      {!accountStatistics && !loadingErrorMessage && <div className="center"><LoadingSpinner /></div>}

      {statisticTiles && (
        <div className="stat-grid">
          {statisticTiles.map((statisticTile) => (
            <Link key={statisticTile.label} to={statisticTile.userListPath} className="card stat-tile">
              <span className="stat-value">{statisticTile.count}</span>
              <span className="stat-label">{statisticTile.label}</span>
            </Link>
          ))}
        </div>
      )}

      <section className="card">
        <h2>Account management</h2>
        <p className="muted">
          Only administrators can view or change other accounts: edit profile data, switch between the
          arborist and official role, lock or unlock access, confirm an e-mail on the user's behalf and delete accounts.
        </p>
        <Link to="/admin/users" className="btn btn-primary">Manage users</Link>
      </section>

      <section className="card">
        <h2>Administrator accounts</h2>
        <p className="muted">
          Administrator access cannot be requested at registration or granted from this panel. To make an
          existing account an administrator, update it directly in the database:
        </p>
        <pre className="code-block">UPDATE arbomachine.user_account SET role = 'ADMIN' WHERE email = 'someone@example.com';</pre>
      </section>
    </div>
  );
}

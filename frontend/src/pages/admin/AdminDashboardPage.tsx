import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { adminPanelApi } from './adminPanelApi';
import type { RolePanelWelcome } from '../../api/apiDataTypes';
import type { UserAccountStatistics } from './adminDataTypes';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';

/** Pulpit admina: kafelki z liczbami kont (klikniecie otwiera przefiltrowana liste) i instrukcja nadawania admina. */
export function AdminDashboardPage() {
  const [panelWelcome, setPanelWelcome] = useState<RolePanelWelcome | null>(null);
  const [accountStatistics, setAccountStatistics] = useState<UserAccountStatistics | null>(null);
  const [loadingErrorMessage, setLoadingErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([adminPanelApi.getAdminPanelWelcome(), adminPanelApi.getUserAccountStatistics()])
      .then(([loadedPanelWelcome, loadedStatistics]) => {
        setPanelWelcome(loadedPanelWelcome);
        setAccountStatistics(loadedStatistics);
      })
      .catch((requestError) =>
        setLoadingErrorMessage(requestError instanceof ApiRequestError ? requestError.message : 'Nieoczekiwany błąd.'));
  }, []);

  const statisticTiles = accountStatistics && [
    { label: 'Wszystkie konta', count: accountStatistics.totalAccounts, userListPath: '/admin/users' },
    { label: 'Arboryści', count: accountStatistics.arboristAccounts, userListPath: '/admin/users?role=ARBORIST' },
    { label: 'Urzędnicy', count: accountStatistics.officialAccounts, userListPath: '/admin/users?role=OFFICIAL' },
    { label: 'Administratorzy', count: accountStatistics.adminAccounts, userListPath: '/admin/users?role=ADMIN' },
    { label: 'Czekają na potwierdzenie e-maila', count: accountStatistics.accountsAwaitingEmailConfirmation, userListPath: '/admin/users?status=unconfirmed' },
    { label: 'Zablokowane', count: accountStatistics.accountsLockedByAdmin, userListPath: '/admin/users?status=locked' },
  ];

  return (
    <div className="stack-lg">
      <header className="page-header">
        <h1>{panelWelcome ? `Witaj, ${panelWelcome.firstName}` : 'Pulpit'}</h1>
        <p className="muted">{panelWelcome?.welcomeMessage ?? 'Przegląd administracyjny.'}</p>
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
        <h2>Zarządzanie kontami</h2>
        <p className="muted">
          Tylko administratorzy mogą przeglądać i zmieniać cudze konta: edytować dane profilu, przełączać rolę
          arborysty i urzędnika, blokować i odblokowywać dostęp, potwierdzać e-mail w imieniu użytkownika i usuwać konta.
        </p>
        <Link to="/admin/users" className="btn btn-primary">Zarządzaj użytkownikami</Link>
      </section>

      <section className="card">
        <h2>Konta administratorów</h2>
        <p className="muted">
          Dostępu administratora nie można uzyskać przy rejestracji ani nadać w tym panelu. Aby istniejące konto
          stało się kontem administratora, zmień je bezpośrednio w bazie danych:
        </p>
        <pre className="code-block">UPDATE arbomachine.user_account SET role = 'ADMIN' WHERE email = 'someone@example.com';</pre>
      </section>
    </div>
  );
}

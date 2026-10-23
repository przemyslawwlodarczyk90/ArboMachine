import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { currentUserAccountApi } from '../api/currentUserAccountApi';
import type { UserProfile } from '../api/apiDataTypes';
import { useAuthentication } from '../auth/AuthenticationContext';
import { USER_ROLE_DISPLAY_NAMES } from '../auth/userRoles';
import { Alert } from '../components/Alert';
import { UserProfileForm } from '../components/UserProfileForm';
import { NotificationList } from '../notifications/NotificationList';
import { PushNotificationsPanel } from '../notifications/PushNotificationsPanel';
import { DocumentAuthorProfileSection } from './documentauthor/DocumentAuthorProfileSection';

/** "My profile" - ta sama strona w kazdym panelu; dziala wylacznie na WLASNYM koncie zalogowanego uzytkownika. */
export function MyProfilePage() {
  const { currentUserAccount, replaceCurrentUserAccount, signOut } = useAuthentication();
  const navigate = useNavigate();

  const [isAccountDeletionConfirmationVisible, setIsAccountDeletionConfirmationVisible] = useState(false);
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);
  const [accountDeletionErrorMessage, setAccountDeletionErrorMessage] = useState<string | null>(null);

  if (!currentUserAccount) return null;

  const saveOwnProfile = async (updatedProfile: UserProfile) => {
    const savedAccount = await currentUserAccountApi.updateCurrentUserProfile(updatedProfile);
    replaceCurrentUserAccount(savedAccount);
    return savedAccount;
  };

  const deleteOwnAccountAndSignOut = async () => {
    setAccountDeletionErrorMessage(null);
    setIsDeletingAccount(true);
    try {
      await currentUserAccountApi.deleteCurrentUserAccount();
      signOut();
      navigate('/login', { replace: true, state: { noticeMessage: 'Konto zostało usunięte.' } });
    } catch (deletionError) {
      setAccountDeletionErrorMessage(deletionError instanceof ApiRequestError ? deletionError.message : 'Nieoczekiwany błąd.');
      setIsDeletingAccount(false);
    }
  };

  return (
    <div className="stack-lg">
      <header className="page-header">
        <h1>Mój profil</h1>
        <p className="muted">
          Zalogowano jako <strong>{currentUserAccount.email}</strong> · konto: {USER_ROLE_DISPLAY_NAMES[currentUserAccount.role]}
        </p>
      </header>

      <section className="card">
        <h2>Dane osobowe</h2>
        <UserProfileForm editedAccount={currentUserAccount} saveProfile={saveOwnProfile} />
      </section>

      {currentUserAccount.role === 'ARBORIST' && <DocumentAuthorProfileSection />}

      <section className="card stack-tight">
        <h2>Powiadomienia</h2>
        <PushNotificationsPanel />
        <h3>Ostatnie powiadomienia</h3>
        <NotificationList />
      </section>

      <section className="card">
        <h2>Hasło</h2>
        <p className="muted">
          Aby zmienić hasło, wyloguj się i użyj opcji „Nie pamiętasz hasła?” na stronie logowania -
          wyślemy e-mailem link do ustawienia nowego.
        </p>
      </section>

      <section className="card card-danger">
        <h2>Usuń konto</h2>
        <p className="muted">Konto i wszystkie powiązane dane zostaną trwale usunięte. Tej operacji nie można cofnąć.</p>
        {accountDeletionErrorMessage && <Alert variant="error">{accountDeletionErrorMessage}</Alert>}
        {isAccountDeletionConfirmationVisible ? (
          <div className="form-actions">
            <button className="btn btn-ghost" onClick={() => setIsAccountDeletionConfirmationVisible(false)}
                    disabled={isDeletingAccount}>
              Anuluj
            </button>
            <button className="btn btn-danger" onClick={deleteOwnAccountAndSignOut} disabled={isDeletingAccount}>
              {isDeletingAccount ? 'Usuwanie…' : 'Tak, usuń moje konto'}
            </button>
          </div>
        ) : (
          <button className="btn btn-danger-outline" onClick={() => setIsAccountDeletionConfirmationVisible(true)}>
            Delete account
          </button>
        )}
      </section>
    </div>
  );
}

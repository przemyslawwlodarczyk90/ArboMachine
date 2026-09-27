import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { currentUserAccountApi } from '../api/currentUserAccountApi';
import type { UserProfile } from '../api/apiDataTypes';
import { useAuthentication } from '../auth/AuthenticationContext';
import { USER_ROLE_DISPLAY_NAMES } from '../auth/userRoles';
import { Alert } from '../components/Alert';
import { UserProfileForm } from '../components/UserProfileForm';

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
      navigate('/login', { replace: true, state: { noticeMessage: 'Your account has been deleted.' } });
    } catch (deletionError) {
      setAccountDeletionErrorMessage(deletionError instanceof ApiRequestError ? deletionError.message : 'Unexpected error.');
      setIsDeletingAccount(false);
    }
  };

  return (
    <div className="stack-lg">
      <header className="page-header">
        <h1>My profile</h1>
        <p className="muted">
          Signed in as <strong>{currentUserAccount.email}</strong> · {USER_ROLE_DISPLAY_NAMES[currentUserAccount.role]} account
        </p>
      </header>

      <section className="card">
        <h2>Personal details</h2>
        <UserProfileForm editedAccount={currentUserAccount} saveProfile={saveOwnProfile} />
      </section>

      <section className="card">
        <h2>Password</h2>
        <p className="muted">
          To change your password, sign out and use “Forgot your password?” on the sign-in page -
          we will e-mail you a link to set a new one.
        </p>
      </section>

      <section className="card card-danger">
        <h2>Delete account</h2>
        <p className="muted">Your account and all related data will be permanently removed. This cannot be undone.</p>
        {accountDeletionErrorMessage && <Alert variant="error">{accountDeletionErrorMessage}</Alert>}
        {isAccountDeletionConfirmationVisible ? (
          <div className="form-actions">
            <button className="btn btn-ghost" onClick={() => setIsAccountDeletionConfirmationVisible(false)}
                    disabled={isDeletingAccount}>
              Cancel
            </button>
            <button className="btn btn-danger" onClick={deleteOwnAccountAndSignOut} disabled={isDeletingAccount}>
              {isDeletingAccount ? 'Deleting…' : 'Yes, delete my account'}
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

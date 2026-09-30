import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { authenticationApi } from '../api/authenticationApi';
import { Alert } from '../components/Alert';
import { AuthCardPageLayout } from '../components/AuthCardPageLayout';
import { LoadingSpinner } from '../components/LoadingSpinner';

type EmailConfirmationStatus = 'confirming' | 'confirmed' | 'failed';

/** Strona z linku aktywacyjnego (/confirm?token=...) - potwierdza e-mail od razu po otwarciu. */
export function EmailConfirmationPage() {
  const [searchParams] = useSearchParams();
  const confirmationToken = searchParams.get('token');
  const [confirmationStatus, setConfirmationStatus] = useState<EmailConfirmationStatus>(confirmationToken ? 'confirming' : 'failed');
  const [failureMessage, setFailureMessage] = useState(confirmationToken ? '' : 'W linku brakuje tokenu.');
  // Token jest jednorazowy - StrictMode w trybie dev odpala efekt dwa razy i drugie wywolanie dostaloby 404.
  const confirmationRequestAlreadySent = useRef(false);

  useEffect(() => {
    if (!confirmationToken || confirmationRequestAlreadySent.current) return;
    confirmationRequestAlreadySent.current = true;
    authenticationApi.confirmEmailUsingToken(confirmationToken)
      .then(() => setConfirmationStatus('confirmed'))
      .catch((confirmationError) => {
        setConfirmationStatus('failed');
        setFailureMessage(confirmationError instanceof ApiRequestError ? confirmationError.message : 'Nieoczekiwany błąd.');
      });
  }, [confirmationToken]);

  return (
    <AuthCardPageLayout title="Aktywacja konta">
      {confirmationStatus === 'confirming' && <div className="center"><LoadingSpinner /></div>}
      {confirmationStatus === 'confirmed' && (
        <>
          <Alert variant="success">Konto jest aktywne. Możesz się zalogować.</Alert>
          <Link to="/login" className="btn btn-primary btn-block">Zaloguj się</Link>
        </>
      )}
      {confirmationStatus === 'failed' && (
        <>
          <Alert variant="error">{failureMessage}</Alert>
          <p className="muted small">
            Jeśli link wygasł, zarejestruj się ponownie na ten sam adres e-mail - wyślemy nowy link aktywacyjny.
            If your account is already active, simply sign in.
          </p>
          <div className="stack">
            <Link to="/login" className="btn btn-primary btn-block">Zaloguj się</Link>
            <Link to="/register" className="btn btn-secondary btn-block">Wróć do rejestracji</Link>
          </div>
        </>
      )}
    </AuthCardPageLayout>
  );
}

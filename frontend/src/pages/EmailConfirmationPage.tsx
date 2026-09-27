import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ApiRequestError, arboMachineApi } from '../api/arboMachineApiClient';
import { Alert } from '../components/Alert';
import { AuthCardPageLayout } from '../components/AuthCardPageLayout';
import { LoadingSpinner } from '../components/LoadingSpinner';

type EmailConfirmationStatus = 'confirming' | 'confirmed' | 'failed';

/** Strona z linku aktywacyjnego (/confirm?token=...) - potwierdza e-mail od razu po otwarciu. */
export function EmailConfirmationPage() {
  const [searchParams] = useSearchParams();
  const confirmationToken = searchParams.get('token');
  const [confirmationStatus, setConfirmationStatus] = useState<EmailConfirmationStatus>(confirmationToken ? 'confirming' : 'failed');
  const [failureMessage, setFailureMessage] = useState(confirmationToken ? '' : 'The link is missing its token.');
  // Token jest jednorazowy - StrictMode w trybie dev odpala efekt dwa razy i drugie wywolanie dostaloby 404.
  const confirmationRequestAlreadySent = useRef(false);

  useEffect(() => {
    if (!confirmationToken || confirmationRequestAlreadySent.current) return;
    confirmationRequestAlreadySent.current = true;
    arboMachineApi.confirmEmailUsingToken(confirmationToken)
      .then(() => setConfirmationStatus('confirmed'))
      .catch((confirmationError) => {
        setConfirmationStatus('failed');
        setFailureMessage(confirmationError instanceof ApiRequestError ? confirmationError.message : 'Unexpected error.');
      });
  }, [confirmationToken]);

  return (
    <AuthCardPageLayout title="Account activation">
      {confirmationStatus === 'confirming' && <div className="center"><LoadingSpinner /></div>}
      {confirmationStatus === 'confirmed' && (
        <>
          <Alert variant="success">Your account is now active. You can sign in.</Alert>
          <Link to="/login" className="btn btn-primary btn-block">Sign in</Link>
        </>
      )}
      {confirmationStatus === 'failed' && (
        <>
          <Alert variant="error">{failureMessage}</Alert>
          <p className="muted small">
            If the link has expired, register again with the same e-mail address and we'll send a new activation link.
            If your account is already active, simply sign in.
          </p>
          <div className="stack">
            <Link to="/login" className="btn btn-primary btn-block">Sign in</Link>
            <Link to="/register" className="btn btn-secondary btn-block">Back to registration</Link>
          </div>
        </>
      )}
    </AuthCardPageLayout>
  );
}

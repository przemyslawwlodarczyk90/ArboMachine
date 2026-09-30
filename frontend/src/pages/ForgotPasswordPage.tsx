import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { authenticationApi } from '../api/authenticationApi';
import { Alert } from '../components/Alert';
import { AuthCardPageLayout } from '../components/AuthCardPageLayout';
import { LabeledInputField } from '../components/LabeledInputField';

/** Prosba o link do resetu hasla. Komunikat po wyslaniu jest zawsze ten sam - nie zdradza, czy konto istnieje. */
export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [emailLinkWasSentTo, setEmailLinkWasSentTo] = useState<string | null>(null);
  const [requestError, setRequestError] = useState<ApiRequestError | null>(null);
  const [isSending, setIsSending] = useState(false);

  const submitResetLinkRequest = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    setRequestError(null);
    setIsSending(true);
    try {
      await authenticationApi.requestPasswordResetLink(email.trim());
      setEmailLinkWasSentTo(email.trim());
    } catch (sendingError) {
      setRequestError(sendingError instanceof ApiRequestError ? sendingError : new ApiRequestError(0, 'Nieoczekiwany błąd.'));
    } finally {
      setIsSending(false);
    }
  };

  return (
    <AuthCardPageLayout
      title="Reset hasła"
      subtitle="Podaj e-mail konta - wyślemy link do ustawienia nowego hasła."
      footer={<Link to="/login">Wróć do logowania</Link>}
    >
      {emailLinkWasSentTo ? (
        <>
          <Alert variant="success">
            Jeśli istnieje aktywne konto dla <strong>{emailLinkWasSentTo}</strong>, wysłaliśmy na nie link do resetu hasła.
          </Alert>
          <p className="muted small">Link jest ważny 30 minut. Sprawdź też folder spam.</p>
          <button className="btn btn-secondary btn-block" onClick={() => setEmailLinkWasSentTo(null)}>Wyślij ponownie</button>
        </>
      ) : (
        <form onSubmit={submitResetLinkRequest} className="form" noValidate>
          {requestError && !requestError.errorMessageByFieldName.email && <Alert variant="error">{requestError.message}</Alert>}
          <LabeledInputField label="E-mail" type="email" autoComplete="email" autoFocus value={email}
                             onChange={(changeEvent) => setEmail(changeEvent.target.value)}
                             errorMessage={requestError?.errorMessageByFieldName.email} />
          <button type="submit" className="btn btn-primary btn-block" disabled={isSending || !email}>
            {isSending ? 'Wysyłanie…' : 'Wyślij link'}
          </button>
        </form>
      )}
    </AuthCardPageLayout>
  );
}

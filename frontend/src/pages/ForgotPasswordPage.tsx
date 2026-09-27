import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError, arboMachineApi } from '../api/arboMachineApiClient';
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
      await arboMachineApi.requestPasswordResetLink(email.trim());
      setEmailLinkWasSentTo(email.trim());
    } catch (sendingError) {
      setRequestError(sendingError instanceof ApiRequestError ? sendingError : new ApiRequestError(0, 'Unexpected error.'));
    } finally {
      setIsSending(false);
    }
  };

  return (
    <AuthCardPageLayout
      title="Reset your password"
      subtitle="Enter your account's e-mail and we'll send you a link to set a new password."
      footer={<Link to="/login">Back to sign in</Link>}
    >
      {emailLinkWasSentTo ? (
        <>
          <Alert variant="success">
            If an active account exists for <strong>{emailLinkWasSentTo}</strong>, we've sent a password reset link to it.
          </Alert>
          <p className="muted small">The link is valid for 30 minutes. Check your spam folder too.</p>
          <button className="btn btn-secondary btn-block" onClick={() => setEmailLinkWasSentTo(null)}>Send again</button>
        </>
      ) : (
        <form onSubmit={submitResetLinkRequest} className="form" noValidate>
          {requestError && !requestError.errorMessageByFieldName.email && <Alert variant="error">{requestError.message}</Alert>}
          <LabeledInputField label="E-mail" type="email" autoComplete="email" autoFocus value={email}
                             onChange={(changeEvent) => setEmail(changeEvent.target.value)}
                             errorMessage={requestError?.errorMessageByFieldName.email} />
          <button type="submit" className="btn btn-primary btn-block" disabled={isSending || !email}>
            {isSending ? 'Sending…' : 'Send reset link'}
          </button>
        </form>
      )}
    </AuthCardPageLayout>
  );
}

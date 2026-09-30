import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { authenticationApi } from '../api/authenticationApi';
import { Alert } from '../components/Alert';
import { AuthCardPageLayout } from '../components/AuthCardPageLayout';
import { LabeledInputField } from '../components/LabeledInputField';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { MINIMUM_PASSWORD_LENGTH, validateNewPasswordAndRepetition } from './newPasswordValidation';

type ResetTokenValidationStatus = 'checking' | 'valid' | 'invalid';

/** Strona z linku resetu (/reset-password?token=...): najpierw sprawdza link, potem pokazuje formularz. */
export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const resetToken = searchParams.get('token') ?? '';
  const navigate = useNavigate();

  const [tokenValidationStatus, setTokenValidationStatus] = useState<ResetTokenValidationStatus>(resetToken ? 'checking' : 'invalid');
  const [invalidTokenMessage, setInvalidTokenMessage] = useState(resetToken ? '' : 'W linku brakuje tokenu.');
  const [newPassword, setNewPassword] = useState('');
  const [repeatedPassword, setRepeatedPassword] = useState('');
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (!resetToken) return;
    authenticationApi.validatePasswordResetToken(resetToken)
      .then(() => setTokenValidationStatus('valid'))
      .catch((validationError) => {
        setTokenValidationStatus('invalid');
        setInvalidTokenMessage(validationError instanceof ApiRequestError ? validationError.message : 'Nieoczekiwany błąd.');
      });
  }, [resetToken]);

  const submitNewPassword = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    setFormErrorMessage(null);
    const clientSideErrors = validateNewPasswordAndRepetition(newPassword, repeatedPassword, 'newPassword');
    setErrorMessageByFieldName(clientSideErrors);
    if (Object.keys(clientSideErrors).length) return;

    setIsSaving(true);
    try {
      await authenticationApi.resetPasswordUsingToken(resetToken, newPassword);
      navigate('/login', {
        replace: true,
        state: { noticeMessage: 'Hasło zostało zmienione. Zaloguj się nowym hasłem.' },
      });
    } catch (resetError) {
      if (resetError instanceof ApiRequestError) {
        setErrorMessageByFieldName(resetError.errorMessageByFieldName);
        setFormErrorMessage(resetError.message);
      } else {
        setFormErrorMessage('Nieoczekiwany błąd.');
      }
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <AuthCardPageLayout title="Ustaw nowe hasło" footer={<Link to="/login">Wróć do logowania</Link>}>
      {tokenValidationStatus === 'checking' && <div className="center"><LoadingSpinner /></div>}
      {tokenValidationStatus === 'invalid' && (
        <>
          <Alert variant="error">{invalidTokenMessage}</Alert>
          <Link to="/forgot-password" className="btn btn-secondary btn-block">Wyślij nowy link</Link>
        </>
      )}
      {tokenValidationStatus === 'valid' && (
        <form onSubmit={submitNewPassword} className="form" noValidate>
          {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
          <LabeledInputField label="Nowe hasło" type="password" autoComplete="new-password" autoFocus value={newPassword}
                             onChange={(changeEvent) => setNewPassword(changeEvent.target.value)}
                             errorMessage={errorMessageByFieldName.newPassword}
                             hintText={`Co najmniej ${MINIMUM_PASSWORD_LENGTH} znaków.`} />
          <LabeledInputField label="Powtórz nowe hasło" type="password" autoComplete="new-password" value={repeatedPassword}
                             onChange={(changeEvent) => setRepeatedPassword(changeEvent.target.value)}
                             errorMessage={errorMessageByFieldName.repeatedPassword} />
          <button type="submit" className="btn btn-primary btn-block" disabled={isSaving}>
            {isSaving ? 'Zapisywanie…' : 'Zmień hasło'}
          </button>
        </form>
      )}
    </AuthCardPageLayout>
  );
}

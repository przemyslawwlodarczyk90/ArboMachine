import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ApiRequestError, arboMachineApi } from '../api/arboMachineApiClient';
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
  const [invalidTokenMessage, setInvalidTokenMessage] = useState(resetToken ? '' : 'The link is missing its token.');
  const [newPassword, setNewPassword] = useState('');
  const [repeatedPassword, setRepeatedPassword] = useState('');
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (!resetToken) return;
    arboMachineApi.validatePasswordResetToken(resetToken)
      .then(() => setTokenValidationStatus('valid'))
      .catch((validationError) => {
        setTokenValidationStatus('invalid');
        setInvalidTokenMessage(validationError instanceof ApiRequestError ? validationError.message : 'Unexpected error.');
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
      await arboMachineApi.resetPasswordUsingToken(resetToken, newPassword);
      navigate('/login', {
        replace: true,
        state: { noticeMessage: 'Your password has been changed. Sign in with the new password.' },
      });
    } catch (resetError) {
      if (resetError instanceof ApiRequestError) {
        setErrorMessageByFieldName(resetError.errorMessageByFieldName);
        setFormErrorMessage(resetError.message);
      } else {
        setFormErrorMessage('Unexpected error.');
      }
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <AuthCardPageLayout title="Set a new password" footer={<Link to="/login">Back to sign in</Link>}>
      {tokenValidationStatus === 'checking' && <div className="center"><LoadingSpinner /></div>}
      {tokenValidationStatus === 'invalid' && (
        <>
          <Alert variant="error">{invalidTokenMessage}</Alert>
          <Link to="/forgot-password" className="btn btn-secondary btn-block">Send a new link</Link>
        </>
      )}
      {tokenValidationStatus === 'valid' && (
        <form onSubmit={submitNewPassword} className="form" noValidate>
          {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
          <LabeledInputField label="New password" type="password" autoComplete="new-password" autoFocus value={newPassword}
                             onChange={(changeEvent) => setNewPassword(changeEvent.target.value)}
                             errorMessage={errorMessageByFieldName.newPassword}
                             hintText={`At least ${MINIMUM_PASSWORD_LENGTH} characters.`} />
          <LabeledInputField label="Repeat new password" type="password" autoComplete="new-password" value={repeatedPassword}
                             onChange={(changeEvent) => setRepeatedPassword(changeEvent.target.value)}
                             errorMessage={errorMessageByFieldName.repeatedPassword} />
          <button type="submit" className="btn btn-primary btn-block" disabled={isSaving}>
            {isSaving ? 'Saving…' : 'Change password'}
          </button>
        </form>
      )}
    </AuthCardPageLayout>
  );
}

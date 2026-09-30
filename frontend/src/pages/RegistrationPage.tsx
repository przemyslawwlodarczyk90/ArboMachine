import { useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { authenticationApi } from '../api/authenticationApi';
import type { AssignableUserRole, UserProfile } from '../api/apiDataTypes';
import { REGISTRATION_ACCOUNT_TYPE_OPTIONS } from '../auth/userRoles';
import { Alert } from '../components/Alert';
import { AuthCardPageLayout } from '../components/AuthCardPageLayout';
import { LabeledInputField } from '../components/LabeledInputField';
import { toUserProfileRequestPayload } from './userProfileRequestPayload';
import { MINIMUM_PASSWORD_LENGTH, validateNewPasswordAndRepetition } from './newPasswordValidation';

const EMPTY_PROFILE_FORM_VALUES: UserProfile = {
  firstName: '', lastName: '', email: '', phoneNumber: '', street: '', postalCode: '', city: '',
};

export function RegistrationPage() {
  const [selectedAccountType, setSelectedAccountType] = useState<AssignableUserRole | null>(null);
  const [profileFormValues, setProfileFormValues] = useState<UserProfile>(EMPTY_PROFILE_FORM_VALUES);
  const [password, setPassword] = useState('');
  const [repeatedPassword, setRepeatedPassword] = useState('');
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [formErrorMessage, setFormErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);

  const updateProfileField = (fieldName: keyof UserProfile) => (changeEvent: ChangeEvent<HTMLInputElement>) =>
    setProfileFormValues((previousValues) => ({ ...previousValues, [fieldName]: changeEvent.target.value }));

  const submitRegistration = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSubmissionInProgress.current) return;
    setFormErrorMessage(null);

    // Najpierw walidacja po stronie przegladarki - szybka informacja bez wysylania zadania.
    const clientSideErrors: Record<string, string> = {};
    if (!selectedAccountType) clientSideErrors.accountType = 'Wybierz typ konta.';
    if (!profileFormValues.firstName.trim()) clientSideErrors.firstName = 'Imię jest wymagane.';
    if (!profileFormValues.lastName.trim()) clientSideErrors.lastName = 'Nazwisko jest wymagane.';
    if (!profileFormValues.email.trim()) clientSideErrors.email = 'E-mail jest wymagany.';
    Object.assign(clientSideErrors, validateNewPasswordAndRepetition(password, repeatedPassword, 'password'));
    setErrorMessageByFieldName(clientSideErrors);
    if (Object.keys(clientSideErrors).length || !selectedAccountType) return;

    isSubmissionInProgress.current = true;
    setIsSubmitting(true);
    try {
      await authenticationApi.registerNewUserAccount({
        profile: toUserProfileRequestPayload(profileFormValues),
        password,
        accountType: selectedAccountType,
      });
      setRegisteredEmail(profileFormValues.email.trim());
    } catch (registrationError) {
      if (registrationError instanceof ApiRequestError) {
        setErrorMessageByFieldName(registrationError.errorMessageByFieldName);
        setFormErrorMessage(registrationError.message);
      } else {
        setFormErrorMessage('Nieoczekiwany błąd.');
      }
    } finally {
      isSubmissionInProgress.current = false;
      setIsSubmitting(false);
    }
  };

  if (registeredEmail) {
    return (
      <AuthCardPageLayout title="Sprawdź skrzynkę e-mail" footer={<Link to="/login">Przejdź do logowania</Link>}>
        <Alert variant="success">
          Konto zostało utworzone. Link aktywacyjny wysłaliśmy na adres <strong>{registeredEmail}</strong>.
        </Alert>
        <p className="muted small">
          Link jest ważny 24 godziny. Jeśli wiadomość nie dotrze, sprawdź folder spam albo zarejestruj się ponownie
          na ten sam adres - wyślemy nowy link. Jeśli potwierdzanie e-maila jest wyłączone w konfiguracji serwera,
          możesz zalogować się od razu.
        </p>
      </AuthCardPageLayout>
    );
  }

  return (
    <AuthCardPageLayout
      title="Załóż konto"
      subtitle="Wyślemy link aktywacyjny na Twój adres e-mail."
      footer={<>Masz już konto? <Link to="/login">Zaloguj się</Link></>}
    >
      {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
      <form onSubmit={submitRegistration} className="form" noValidate>
        <fieldset className="choice-group">
          <legend>Typ konta *</legend>
          <div className="choice-grid">
            {REGISTRATION_ACCOUNT_TYPE_OPTIONS.map((accountTypeOption) => (
              <label key={accountTypeOption.role}
                     className={`choice-card${selectedAccountType === accountTypeOption.role ? ' selected' : ''}`}>
                <input type="radio" name="accountType" value={accountTypeOption.role}
                       checked={selectedAccountType === accountTypeOption.role}
                       onChange={() => setSelectedAccountType(accountTypeOption.role)} />
                <span className="choice-title">{accountTypeOption.displayName}</span>
                <span className="choice-desc">{accountTypeOption.description}</span>
              </label>
            ))}
          </div>
          {errorMessageByFieldName.accountType && <span className="field-error">{errorMessageByFieldName.accountType}</span>}
        </fieldset>

        <div className="grid-2">
          <LabeledInputField label="Imię *" autoComplete="given-name" value={profileFormValues.firstName}
                             onChange={updateProfileField('firstName')} errorMessage={errorMessageByFieldName.firstName} />
          <LabeledInputField label="Nazwisko *" autoComplete="family-name" value={profileFormValues.lastName}
                             onChange={updateProfileField('lastName')} errorMessage={errorMessageByFieldName.lastName} />
        </div>
        <LabeledInputField label="E-mail *" type="email" autoComplete="email" value={profileFormValues.email}
                           onChange={updateProfileField('email')} errorMessage={errorMessageByFieldName.email} />
        <LabeledInputField label="Telefon" type="tel" autoComplete="tel" value={profileFormValues.phoneNumber ?? ''}
                           onChange={updateProfileField('phoneNumber')} errorMessage={errorMessageByFieldName.phoneNumber}
                           placeholder="+48 123 456 789" />
        <LabeledInputField label="Ulica" autoComplete="street-address" value={profileFormValues.street ?? ''}
                           onChange={updateProfileField('street')} errorMessage={errorMessageByFieldName.street} />
        <div className="grid-2">
          <LabeledInputField label="Kod pocztowy" autoComplete="postal-code" value={profileFormValues.postalCode ?? ''}
                             onChange={updateProfileField('postalCode')} errorMessage={errorMessageByFieldName.postalCode}
                             placeholder="00-000" />
          <LabeledInputField label="Miejscowość" autoComplete="address-level2" value={profileFormValues.city ?? ''}
                             onChange={updateProfileField('city')} errorMessage={errorMessageByFieldName.city} />
        </div>
        <div className="grid-2">
          <LabeledInputField label="Hasło *" type="password" autoComplete="new-password" value={password}
                             onChange={(changeEvent) => setPassword(changeEvent.target.value)}
                             errorMessage={errorMessageByFieldName.password}
                             hintText={`Co najmniej ${MINIMUM_PASSWORD_LENGTH} znaków.`} />
          <LabeledInputField label="Powtórz hasło *" type="password" autoComplete="new-password" value={repeatedPassword}
                             onChange={(changeEvent) => setRepeatedPassword(changeEvent.target.value)}
                             errorMessage={errorMessageByFieldName.repeatedPassword} />
        </div>
        <button type="submit" className="btn btn-primary btn-block" disabled={isSubmitting}>
          {isSubmitting ? 'Zakładanie konta…' : 'Załóż konto'}
        </button>
      </form>
    </AuthCardPageLayout>
  );
}

import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError, arboMachineApi } from '../api/arboMachineApiClient';
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

  const updateProfileField = (fieldName: keyof UserProfile) => (changeEvent: ChangeEvent<HTMLInputElement>) =>
    setProfileFormValues((previousValues) => ({ ...previousValues, [fieldName]: changeEvent.target.value }));

  const submitRegistration = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    setFormErrorMessage(null);

    // Najpierw walidacja po stronie przegladarki - szybka informacja bez wysylania zadania.
    const clientSideErrors: Record<string, string> = {};
    if (!selectedAccountType) clientSideErrors.accountType = 'Choose an account type.';
    if (!profileFormValues.firstName.trim()) clientSideErrors.firstName = 'First name is required.';
    if (!profileFormValues.lastName.trim()) clientSideErrors.lastName = 'Last name is required.';
    if (!profileFormValues.email.trim()) clientSideErrors.email = 'E-mail is required.';
    Object.assign(clientSideErrors, validateNewPasswordAndRepetition(password, repeatedPassword, 'password'));
    setErrorMessageByFieldName(clientSideErrors);
    if (Object.keys(clientSideErrors).length || !selectedAccountType) return;

    setIsSubmitting(true);
    try {
      await arboMachineApi.registerNewUserAccount({
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
        setFormErrorMessage('Unexpected error.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (registeredEmail) {
    return (
      <AuthCardPageLayout title="Check your inbox" footer={<Link to="/login">Go to sign in</Link>}>
        <Alert variant="success">
          Your account has been created. We sent an activation link to <strong>{registeredEmail}</strong>.
        </Alert>
        <p className="muted small">
          The link is valid for 24 hours. If the message doesn't arrive, check your spam folder or register
          again with the same address and we'll send a new link. If e-mail confirmation is disabled in the
          backend configuration, you can sign in right away.
        </p>
      </AuthCardPageLayout>
    );
  }

  return (
    <AuthCardPageLayout
      title="Create an account"
      subtitle="We'll send an activation link to your e-mail address."
      footer={<>Already have an account? <Link to="/login">Sign in</Link></>}
    >
      {formErrorMessage && <Alert variant="error">{formErrorMessage}</Alert>}
      <form onSubmit={submitRegistration} className="form" noValidate>
        <fieldset className="choice-group">
          <legend>Account type *</legend>
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
          <LabeledInputField label="First name *" autoComplete="given-name" value={profileFormValues.firstName}
                             onChange={updateProfileField('firstName')} errorMessage={errorMessageByFieldName.firstName} />
          <LabeledInputField label="Last name *" autoComplete="family-name" value={profileFormValues.lastName}
                             onChange={updateProfileField('lastName')} errorMessage={errorMessageByFieldName.lastName} />
        </div>
        <LabeledInputField label="E-mail *" type="email" autoComplete="email" value={profileFormValues.email}
                           onChange={updateProfileField('email')} errorMessage={errorMessageByFieldName.email} />
        <LabeledInputField label="Phone" type="tel" autoComplete="tel" value={profileFormValues.phoneNumber ?? ''}
                           onChange={updateProfileField('phoneNumber')} errorMessage={errorMessageByFieldName.phoneNumber}
                           placeholder="+48 123 456 789" />
        <LabeledInputField label="Street" autoComplete="street-address" value={profileFormValues.street ?? ''}
                           onChange={updateProfileField('street')} errorMessage={errorMessageByFieldName.street} />
        <div className="grid-2">
          <LabeledInputField label="Postal code" autoComplete="postal-code" value={profileFormValues.postalCode ?? ''}
                             onChange={updateProfileField('postalCode')} errorMessage={errorMessageByFieldName.postalCode}
                             placeholder="00-000" />
          <LabeledInputField label="City" autoComplete="address-level2" value={profileFormValues.city ?? ''}
                             onChange={updateProfileField('city')} errorMessage={errorMessageByFieldName.city} />
        </div>
        <div className="grid-2">
          <LabeledInputField label="Password *" type="password" autoComplete="new-password" value={password}
                             onChange={(changeEvent) => setPassword(changeEvent.target.value)}
                             errorMessage={errorMessageByFieldName.password}
                             hintText={`At least ${MINIMUM_PASSWORD_LENGTH} characters.`} />
          <LabeledInputField label="Repeat password *" type="password" autoComplete="new-password" value={repeatedPassword}
                             onChange={(changeEvent) => setRepeatedPassword(changeEvent.target.value)}
                             errorMessage={errorMessageByFieldName.repeatedPassword} />
        </div>
        <button type="submit" className="btn btn-primary btn-block" disabled={isSubmitting}>
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </AuthCardPageLayout>
  );
}

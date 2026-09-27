import { useState, type ChangeEvent, type FormEvent } from 'react';
import { ApiRequestError } from '../api/httpClient';
import type { UserAccount, UserProfile } from '../api/apiDataTypes';
import { toUserProfileRequestPayload } from '../pages/userProfileRequestPayload';
import { Alert, type AlertVariant } from './Alert';
import { LabeledInputField } from './LabeledInputField';

/** Wartosci formularza z konta - brakujace pola opcjonalne jako puste napisy (kontrolowane inputy). */
const toProfileFormValues = (userAccount: UserAccount): UserProfile => ({
  firstName: userAccount.firstName,
  lastName: userAccount.lastName,
  email: userAccount.email,
  phoneNumber: userAccount.phoneNumber ?? '',
  street: userAccount.street ?? '',
  postalCode: userAccount.postalCode ?? '',
  city: userAccount.city ?? '',
});

/**
 * Formularz danych profilu - uzywany w "My profile" (wlasne konto) i w oknie edycji admina (cudze konto).
 * Sam zapis robi przekazany `saveProfile`, wiec formularz nie wie, ktory endpoint jest wolany.
 */
export function UserProfileForm({ editedAccount, saveProfile, closeForm, submitButtonLabel = 'Save changes' }: {
  editedAccount: UserAccount;
  saveProfile: (updatedProfile: UserProfile) => Promise<UserAccount>;
  /** podane = formularz w oknie dialogowym, przycisk "Close" zamiast "Discard changes" */
  closeForm?: () => void;
  submitButtonLabel?: string;
}) {
  const [profileFormValues, setProfileFormValues] = useState<UserProfile>(() => toProfileFormValues(editedAccount));
  const [errorMessageByFieldName, setErrorMessageByFieldName] = useState<Record<string, string>>({});
  const [resultMessage, setResultMessage] = useState<{ variant: AlertVariant; text: string } | null>(null);
  const [isSaving, setIsSaving] = useState(false);

  const updateFormField = (fieldName: keyof UserProfile) => (changeEvent: ChangeEvent<HTMLInputElement>) =>
    setProfileFormValues((previousValues) => ({ ...previousValues, [fieldName]: changeEvent.target.value }));

  const hasUnsavedChanges = JSON.stringify(toProfileFormValues(editedAccount)) !== JSON.stringify(profileFormValues);

  const submitProfileChanges = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    setResultMessage(null);
    setErrorMessageByFieldName({});
    setIsSaving(true);
    try {
      const savedAccount = await saveProfile(
        toUserProfileRequestPayload({ ...profileFormValues, email: editedAccount.email }));
      setProfileFormValues(toProfileFormValues(savedAccount));
      setResultMessage({ variant: 'success', text: 'Changes saved.' });
    } catch (savingError) {
      if (savingError instanceof ApiRequestError) {
        setErrorMessageByFieldName(savingError.errorMessageByFieldName);
        setResultMessage({ variant: 'error', text: savingError.message });
      } else {
        setResultMessage({ variant: 'error', text: 'Unexpected error.' });
      }
    } finally {
      setIsSaving(false);
    }
  };

  const discardChangesOrCloseForm = () => {
    setProfileFormValues(toProfileFormValues(editedAccount));
    setErrorMessageByFieldName({});
    setResultMessage(null);
    closeForm?.();
  };

  return (
    <form onSubmit={submitProfileChanges} className="form" noValidate>
      {resultMessage && <Alert variant={resultMessage.variant}>{resultMessage.text}</Alert>}
      <div className="grid-2">
        <LabeledInputField label="First name *" autoComplete="given-name" value={profileFormValues.firstName}
                           onChange={updateFormField('firstName')} errorMessage={errorMessageByFieldName.firstName} />
        <LabeledInputField label="Last name *" autoComplete="family-name" value={profileFormValues.lastName}
                           onChange={updateFormField('lastName')} errorMessage={errorMessageByFieldName.lastName} />
      </div>
      <div className="grid-2">
        <LabeledInputField label="E-mail" type="email" value={editedAccount.email} readOnly disabled
                           hintText="The e-mail address cannot be changed." />
        <LabeledInputField label="Phone" type="tel" autoComplete="tel" value={profileFormValues.phoneNumber ?? ''}
                           onChange={updateFormField('phoneNumber')} errorMessage={errorMessageByFieldName.phoneNumber}
                           placeholder="+48 123 456 789" />
      </div>
      <LabeledInputField label="Street" autoComplete="street-address" value={profileFormValues.street ?? ''}
                         onChange={updateFormField('street')} errorMessage={errorMessageByFieldName.street} />
      <div className="grid-2">
        <LabeledInputField label="Postal code" autoComplete="postal-code" value={profileFormValues.postalCode ?? ''}
                           onChange={updateFormField('postalCode')} errorMessage={errorMessageByFieldName.postalCode}
                           placeholder="00-000" />
        <LabeledInputField label="City" autoComplete="address-level2" value={profileFormValues.city ?? ''}
                           onChange={updateFormField('city')} errorMessage={errorMessageByFieldName.city} />
      </div>
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" disabled={isSaving || (!hasUnsavedChanges && !closeForm)}
                onClick={discardChangesOrCloseForm}>
          {closeForm ? 'Close' : 'Discard changes'}
        </button>
        <button type="submit" className="btn btn-primary" disabled={!hasUnsavedChanges || isSaving}>
          {isSaving ? 'Saving…' : submitButtonLabel}
        </button>
      </div>
    </form>
  );
}

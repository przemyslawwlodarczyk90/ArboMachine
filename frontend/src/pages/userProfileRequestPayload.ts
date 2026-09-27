import type { UserProfile } from '../api/apiDataTypes';

/**
 * Przygotowuje dane profilu do wyslania: przycina spacje i pomija puste pola opcjonalne -
 * inaczej walidacja @Pattern na backendzie (telefon, kod pocztowy) odrzucilaby pusty napis.
 */
export function toUserProfileRequestPayload(formValues: UserProfile): UserProfile {
  const trimmedOrUndefinedWhenBlank = (fieldValue?: string | null) =>
    fieldValue && fieldValue.trim() ? fieldValue.trim() : undefined;
  return {
    id: formValues.id,
    firstName: formValues.firstName.trim(),
    lastName: formValues.lastName.trim(),
    email: formValues.email.trim(),
    phoneNumber: trimmedOrUndefinedWhenBlank(formValues.phoneNumber),
    street: trimmedOrUndefinedWhenBlank(formValues.street),
    postalCode: trimmedOrUndefinedWhenBlank(formValues.postalCode),
    city: trimmedOrUndefinedWhenBlank(formValues.city),
  };
}

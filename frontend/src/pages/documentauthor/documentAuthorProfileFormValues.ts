// Dane autora opracowania (etap 42): pola formularza, zamiana na request i porownanie z zapisanymi danymi.
// Modul bez React - testowany przez node --test.

export type DocumentAuthorProfileField =
  'authorFullName' | 'qualifications' | 'companyName' | 'companyDetails' | 'methodsDescription';

export interface DocumentAuthorProfile extends Partial<Record<DocumentAuthorProfileField, string | null>> {
  hasProfile: boolean;
  updatedAt?: string | null;
}

export type DocumentAuthorProfileRequest = Partial<Record<DocumentAuthorProfileField, string>>;

export type DocumentAuthorProfileFormValues = Record<DocumentAuthorProfileField, string>;

/** Pola w kolejnosci formularza; maxLength = limit backendu (DocumentAuthorProfile). */
export const DOCUMENT_AUTHOR_PROFILE_FIELDS: ReadonlyArray<{
  field: DocumentAuthorProfileField;
  label: string;
  hint: string;
  maxLength: number;
  multiline: boolean;
}> = [
  { field: 'authorFullName', label: 'Autor opracowania', hint: 'Imię i nazwisko, np. mgr inż. Anna Kowalska.', maxLength: 255, multiline: false },
  { field: 'qualifications', label: 'Kwalifikacje autora', hint: 'Np. certyfikaty, uprawnienia, numer legitymacji.', maxLength: 2000, multiline: true },
  { field: 'companyName', label: 'Wykonawca (firma)', hint: 'Nazwa firmy wykonującej opracowanie.', maxLength: 255, multiline: false },
  { field: 'companyDetails', label: 'Dane wykonawcy', hint: 'Adres, NIP, telefon, e-mail - tak, jak mają być w dokumencie.', maxLength: 2000, multiline: true },
  { field: 'methodsDescription', label: 'Metody i techniki pracy', hint: 'Stały opis metod (np. sposób pomiaru, ocena drzew) wstawiany do każdego dokumentu.', maxLength: 10000, multiline: true },
];

export function toDocumentAuthorProfileFormValues(profile: DocumentAuthorProfile | null | undefined): DocumentAuthorProfileFormValues {
  const formValues = {} as DocumentAuthorProfileFormValues;
  for (const { field } of DOCUMENT_AUTHOR_PROFILE_FIELDS) formValues[field] = profile?.[field] ?? '';
  return formValues;
}

/** Komplet danych dla PUT - puste pola pomijane (= brak wartosci), tekst przyciety. Pusty request usuwa dane. */
export function toDocumentAuthorProfileRequest(formValues: DocumentAuthorProfileFormValues): DocumentAuthorProfileRequest {
  const request: DocumentAuthorProfileRequest = {};
  for (const { field } of DOCUMENT_AUTHOR_PROFILE_FIELDS) {
    const trimmed = formValues[field].trim();
    if (trimmed) request[field] = trimmed;
  }
  return request;
}

/** Czy formularz rozni sie od zapisanych danych (niezapisane zmiany). */
export const hasUnsavedDocumentAuthorProfileChanges = (formValues: DocumentAuthorProfileFormValues,
                                                       saved: DocumentAuthorProfile | null | undefined) =>
  JSON.stringify(toDocumentAuthorProfileRequest(formValues))
    !== JSON.stringify(toDocumentAuthorProfileRequest(toDocumentAuthorProfileFormValues(saved)));

/** Bledy pol (za dlugi tekst) - po polsku, przed wyslaniem. */
export function validateDocumentAuthorProfileFormValues(
  formValues: DocumentAuthorProfileFormValues,
): Partial<Record<DocumentAuthorProfileField, string>> {
  const errors: Partial<Record<DocumentAuthorProfileField, string>> = {};
  for (const { field, maxLength } of DOCUMENT_AUTHOR_PROFILE_FIELDS) {
    if (formValues[field].trim().length > maxLength) errors[field] = `Tekst może mieć maksymalnie ${maxLength} znaków.`;
  }
  return errors;
}

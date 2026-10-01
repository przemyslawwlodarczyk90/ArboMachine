import {
  VALUATION_TEXT_FIELDS, type TreeValuation, type TreeValuationCategory, type TreeValuationRequest, type TreeValuationTextField,
} from './valuationDataTypes.ts';

// Wartosci formularza waloryzacji (etap 27): stan pol, zamiana na request i porownanie z zapisana ocena.
// Modul bez React - testowany przez node --test.

export type TreeValuationFormValues = Record<TreeValuationTextField, string> & { overallCategory: TreeValuationCategory | '' };

export const MAXIMUM_VALUATION_TEXT_LENGTH = 5000;

export function toValuationFormValues(valuation: TreeValuation | null | undefined): TreeValuationFormValues {
  const formValues = { overallCategory: valuation?.overallCategory ?? '' } as TreeValuationFormValues;
  for (const { field } of VALUATION_TEXT_FIELDS) formValues[field] = valuation?.[field] ?? '';
  return formValues;
}

/** Komplet oceny dla PUT - puste pola pomijane (= brak wartosci), tekst przyciety. */
export function toValuationRequest(formValues: TreeValuationFormValues): TreeValuationRequest {
  const request: TreeValuationRequest = {};
  if (formValues.overallCategory) request.overallCategory = formValues.overallCategory;
  for (const { field } of VALUATION_TEXT_FIELDS) {
    const trimmed = formValues[field].trim();
    if (trimmed) request[field] = trimmed;
  }
  return request;
}

/** Ocena bez zadnej wartosci - nie da sie jej zapisac (backend: 400); do usuniecia sluzy "Usuń waloryzację". */
export const isValuationEmpty = (formValues: TreeValuationFormValues) => Object.keys(toValuationRequest(formValues)).length === 0;

/** Czy formularz rozni sie od zapisanej oceny (niezapisane zmiany). */
export const hasUnsavedValuationChanges = (formValues: TreeValuationFormValues, saved: TreeValuation | null | undefined) =>
  JSON.stringify(toValuationRequest(formValues)) !== JSON.stringify(toValuationRequest(toValuationFormValues(saved)));

/** Bledy pol (np. za dlugi tekst) - po polsku, przed wyslaniem. */
export function validateValuationFormValues(formValues: TreeValuationFormValues): Partial<Record<TreeValuationTextField, string>> {
  const errors: Partial<Record<TreeValuationTextField, string>> = {};
  for (const { field } of VALUATION_TEXT_FIELDS) {
    if (formValues[field].trim().length > MAXIMUM_VALUATION_TEXT_LENGTH) errors[field] = 'Tekst może mieć maksymalnie 5000 znaków.';
  }
  return errors;
}

import type { Tree, TreeEditableFieldsRequest } from './treeDataTypes';

/**
 * Wartosci formularza drzewa (dodawanie i edycja) - wszystko jako tekst, bo tak dzialaja kontrolowane inputy.
 * Nowe pole drzewa dodajemy tutaj, w TreeFormFields i w TreeDetailsView.
 */
export interface TreeFormValues {
  inventoryNumber: string;
  speciesLatin: string;
  speciesPolish: string;
  notes: string;
}

export const emptyTreeFormValues: TreeFormValues = {
  inventoryNumber: '',
  speciesLatin: '',
  speciesPolish: '',
  notes: '',
};

/** Wartosci formularza z zapisanego drzewa - brakujace pola jako puste napisy. */
export const toTreeFormValues = (tree: Tree): TreeFormValues => ({
  inventoryNumber: tree.inventoryNumber,
  speciesLatin: tree.speciesLatin ?? '',
  speciesPolish: tree.speciesPolish ?? '',
  notes: tree.notes ?? '',
});

const trimmedOrUndefined = (fieldValue: string) => fieldValue.trim() || undefined;

/** Request dla backendu - ta sama normalizacja co na backendzie (trim, puste pole opcjonalne pominiete). */
export const toTreeEditableFieldsRequest = (formValues: TreeFormValues): TreeEditableFieldsRequest => ({
  inventoryNumber: formValues.inventoryNumber.trim(),
  speciesLatin: trimmedOrUndefined(formValues.speciesLatin),
  speciesPolish: trimmedOrUndefined(formValues.speciesPolish),
  notes: trimmedOrUndefined(formValues.notes),
});

/** Czy dwa stany formularza daja ten sam request (np. dopisanie samych spacji nie jest zmiana). */
export const areTreeFormValuesEquivalent = (firstFormValues: TreeFormValues, secondFormValues: TreeFormValues) =>
  JSON.stringify(toTreeEditableFieldsRequest(firstFormValues)) === JSON.stringify(toTreeEditableFieldsRequest(secondFormValues));

/** Walidacja po stronie przegladarki - pusty wynik oznacza, ze formularz mozna wyslac. Backend waliduje ponownie. */
export const validateTreeFormValues = (formValues: TreeFormValues): Record<string, string> => {
  const errorMessageByFieldName: Record<string, string> = {};
  if (!formValues.inventoryNumber.trim()) {
    errorMessageByFieldName.inventoryNumber = 'Inventory number is required.';
  }
  return errorMessageByFieldName;
};

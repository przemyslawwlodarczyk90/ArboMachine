import type { Tree, TreeEditableFieldsRequest } from './treeDataTypes';

/**
 * Wartosci formularza drzewa (dodawanie i edycja) - wszystko jako tekst, bo tak dzialaja kontrolowane inputy.
 * Pomiary tez sa tekstem az do wyslania (pozwala to wpisac przecinek i pokazac czytelny blad zamiast NaN).
 * Nowe pole drzewa dodajemy tutaj, w TreeFormFields i w TreeDetailsView.
 */
export interface TreeFormValues {
  inventoryNumber: string;
  speciesLatin: string;
  speciesPolish: string;
  trunkCircumferenceCm: string;
  heightM: string;
  crownDiameterM: string;
  notes: string;
}

/** Zakresy pomiarow - takie same jak w backendzie (stale w encji Tree). */
const MINIMUM_TRUNK_CIRCUMFERENCE_CM = 1;
const MAXIMUM_TRUNK_CIRCUMFERENCE_CM = 5000;
const MAXIMUM_HEIGHT_M = 150;
const MAXIMUM_CROWN_DIAMETER_M = 200;

const WHOLE_NUMBER_PATTERN = /^\d+$/;
/** Liczba dodatnia z maksymalnie 2 miejscami po kropce lub przecinku (precyzja kolumny numeric(5,2)). */
const METERS_WITH_TWO_DECIMALS_PATTERN = /^\d+([.,]\d{1,2})?$/;

export const emptyTreeFormValues: TreeFormValues = {
  inventoryNumber: '',
  speciesLatin: '',
  speciesPolish: '',
  trunkCircumferenceCm: '',
  heightM: '',
  crownDiameterM: '',
  notes: '',
};

/** Wartosci formularza z zapisanego drzewa - brakujace pola jako puste napisy. */
export const toTreeFormValues = (tree: Tree): TreeFormValues => ({
  inventoryNumber: tree.inventoryNumber,
  speciesLatin: tree.speciesLatin ?? '',
  speciesPolish: tree.speciesPolish ?? '',
  trunkCircumferenceCm: tree.trunkCircumferenceCm?.toString() ?? '',
  heightM: tree.heightM?.toString() ?? '',
  crownDiameterM: tree.crownDiameterM?.toString() ?? '',
  notes: tree.notes ?? '',
});

const trimmedOrUndefined = (fieldValue: string) => fieldValue.trim() || undefined;

/** Tekst pola liczbowego -> liczba; przecinek dziesietny zamieniany na kropke, puste pole = brak wartosci. */
const numberOrUndefined = (fieldValue: string) => {
  const numberText = fieldValue.trim().replace(',', '.');
  return numberText ? Number(numberText) : undefined;
};

/** Request dla backendu - ta sama normalizacja co na backendzie (trim, puste pole opcjonalne pominiete). */
export const toTreeEditableFieldsRequest = (formValues: TreeFormValues): TreeEditableFieldsRequest => ({
  inventoryNumber: formValues.inventoryNumber.trim(),
  speciesLatin: trimmedOrUndefined(formValues.speciesLatin),
  speciesPolish: trimmedOrUndefined(formValues.speciesPolish),
  trunkCircumferenceCm: numberOrUndefined(formValues.trunkCircumferenceCm),
  heightM: numberOrUndefined(formValues.heightM),
  crownDiameterM: numberOrUndefined(formValues.crownDiameterM),
  notes: trimmedOrUndefined(formValues.notes),
});

/** Czy dwa stany formularza daja ten sam request (np. dopisanie samych spacji albo 18.50 zamiast 18.5 nie jest zmiana). */
export const areTreeFormValuesEquivalent = (firstFormValues: TreeFormValues, secondFormValues: TreeFormValues) =>
  JSON.stringify(toTreeEditableFieldsRequest(firstFormValues)) === JSON.stringify(toTreeEditableFieldsRequest(secondFormValues));

/** Blad opcjonalnego pomiaru w metrach albo undefined, gdy pole jest puste lub poprawne. */
const errorOfOptionalMeters = (fieldValue: string, measurementName: string, maximumMeters: number) => {
  const metersText = fieldValue.trim();
  if (!metersText) return undefined;
  if (!METERS_WITH_TWO_DECIMALS_PATTERN.test(metersText)) {
    return `${measurementName} must be a number with at most 2 decimal places.`;
  }
  const meters = Number(metersText.replace(',', '.'));
  return meters > 0 && meters <= maximumMeters
    ? undefined
    : `${measurementName} must be greater than 0 and at most ${maximumMeters} m.`;
};

/** Walidacja po stronie przegladarki - pusty wynik oznacza, ze formularz mozna wyslac. Backend waliduje ponownie. */
export const validateTreeFormValues = (formValues: TreeFormValues): Record<string, string> => {
  const errorMessageByFieldName: Record<string, string> = {};
  if (!formValues.inventoryNumber.trim()) {
    errorMessageByFieldName.inventoryNumber = 'Inventory number is required.';
  }

  const trunkCircumferenceText = formValues.trunkCircumferenceCm.trim();
  if (trunkCircumferenceText && !WHOLE_NUMBER_PATTERN.test(trunkCircumferenceText)) {
    errorMessageByFieldName.trunkCircumferenceCm = 'Trunk circumference must be a whole number of centimeters.';
  } else if (trunkCircumferenceText) {
    const trunkCircumferenceCm = Number(trunkCircumferenceText);
    if (trunkCircumferenceCm < MINIMUM_TRUNK_CIRCUMFERENCE_CM || trunkCircumferenceCm > MAXIMUM_TRUNK_CIRCUMFERENCE_CM) {
      errorMessageByFieldName.trunkCircumferenceCm = 'Trunk circumference must be between 1 and 5000 cm.';
    }
  }

  const heightError = errorOfOptionalMeters(formValues.heightM, 'Height', MAXIMUM_HEIGHT_M);
  if (heightError) errorMessageByFieldName.heightM = heightError;

  const crownDiameterError = errorOfOptionalMeters(formValues.crownDiameterM, 'Crown diameter', MAXIMUM_CROWN_DIAMETER_M);
  if (crownDiameterError) errorMessageByFieldName.crownDiameterM = crownDiameterError;
  return errorMessageByFieldName;
};

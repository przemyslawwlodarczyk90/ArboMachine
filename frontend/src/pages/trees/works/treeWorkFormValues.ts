import type { TreeWork, TreeWorkFieldsRequest, TreeWorkPriority, TreeWorkStatus } from './treeWorkDataTypes';

// Wartosci i walidacja formularza pracy (dodawanie i edycja). Walidacja w przegladarce tylko poprawia UX -
// backend waliduje ponownie. Modul bez importow wartosci - testowany przez node --test.

export interface TreeWorkFormValues {
  name: string;
  description: string;
  status: TreeWorkStatus;
  /** '' = nie okreslono */
  priority: TreeWorkPriority | '';
  /** 'YYYY-MM-DD' albo '' (input type="date") */
  plannedDate: string;
  completedDate: string;
}

/** Limity jak w backendzie (TreeWork). */
export const MAXIMUM_TREE_WORK_NAME_LENGTH = 255;
export const MAXIMUM_TREE_WORK_DESCRIPTION_LENGTH = 5000;

export const emptyTreeWorkFormValues: TreeWorkFormValues = {
  name: '', description: '', status: 'RECOMMENDED', priority: '', plannedDate: '', completedDate: '',
};

export const toTreeWorkFormValues = (treeWork: TreeWork): TreeWorkFormValues => ({
  name: treeWork.name,
  description: treeWork.description ?? '',
  status: treeWork.status ?? 'RECOMMENDED',
  priority: treeWork.priority ?? '',
  plannedDate: treeWork.plannedDate ?? '',
  completedDate: treeWork.completedDate ?? '',
});

/** Ta sama normalizacja co w backendzie: nazwa przycieta, pusty opis pominiety. */
export const toTreeWorkFieldsRequest = (formValues: TreeWorkFormValues): TreeWorkFieldsRequest => ({
  name: formValues.name.trim(),
  description: formValues.description.trim() || undefined,
  status: formValues.status,
  priority: formValues.priority || undefined,
  plannedDate: formValues.plannedDate || undefined,
  completedDate: formValues.completedDate || undefined,
});

/**
 * "Oznacz jako wykonaną" (etap 28): komplet pol pracy (PATCH = ostatni zapis wygrywa) ze statusem COMPLETED;
 * data wykonania = dotychczasowa albo dzisiejsza.
 */
export const toCompletedTreeWorkRequest = (treeWork: TreeWork, today: string): TreeWorkFieldsRequest => ({
  ...toTreeWorkFieldsRequest(toTreeWorkFormValues(treeWork)),
  status: 'COMPLETED',
  completedDate: treeWork.completedDate || today,
});

/** Czy dwa stany formularza daja ten sam request (np. dopisanie samych spacji nie jest zmiana). */
export const areTreeWorkFormValuesEquivalent = (first: TreeWorkFormValues, second: TreeWorkFormValues) =>
  JSON.stringify(toTreeWorkFieldsRequest(first)) === JSON.stringify(toTreeWorkFieldsRequest(second));

/** Pusty wynik oznacza, ze formularz mozna wyslac. */
export const validateTreeWorkFormValues = (formValues: TreeWorkFormValues): Record<string, string> => {
  const errorMessageByFieldName: Record<string, string> = {};
  const trimmedName = formValues.name.trim();
  if (!trimmedName) {
    errorMessageByFieldName.name = 'Nazwa jest wymagana.';
  } else if (trimmedName.length > MAXIMUM_TREE_WORK_NAME_LENGTH) {
    errorMessageByFieldName.name = `Nazwa może mieć maksymalnie ${MAXIMUM_TREE_WORK_NAME_LENGTH} znaków.`;
  }
  if (formValues.description.trim().length > MAXIMUM_TREE_WORK_DESCRIPTION_LENGTH) {
    errorMessageByFieldName.description = `Opis może mieć maksymalnie ${MAXIMUM_TREE_WORK_DESCRIPTION_LENGTH} znaków.`;
  }
  // Daty bez walidacji kolejnosci i "nie w przeszlosci" (etap 28) - historyczne wpisy sa dozwolone.
  for (const dateField of ['plannedDate', 'completedDate'] as const) {
    if (formValues[dateField] && !/^\d{4}-\d{2}-\d{2}$/.test(formValues[dateField])) {
      errorMessageByFieldName[dateField] = 'Podaj poprawną datę.';
    }
  }
  return errorMessageByFieldName;
};

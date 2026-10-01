import type { AnnotationElement } from './annotationDataTypes';

// Cofnij/Ponow w biezacej sesji edycji - prosty stos stanow listy elementow. Historia NIE trafia do backendu:
// po zapisie przechowywany jest tylko aktualny dokument. Modul bez importow wartosci - node --test.

export interface AnnotationHistory {
  past: AnnotationElement[][];
  present: AnnotationElement[];
  future: AnnotationElement[][];
}

export const createAnnotationHistory = (initialElements: AnnotationElement[]): AnnotationHistory =>
  ({ past: [], present: initialElements, future: [] });

/** Nowy stan (np. dodany element albo "Wyczysc") - czysci mozliwosc "Ponow". */
export const pushAnnotationState = (history: AnnotationHistory, newElements: AnnotationElement[]): AnnotationHistory =>
  ({ past: [...history.past, history.present], present: newElements, future: [] });

export const addAnnotationElement = (history: AnnotationHistory, element: AnnotationElement): AnnotationHistory =>
  pushAnnotationState(history, [...history.present, element]);

/** "Wyczysc" to zwykly krok historii - mozna go cofnac. Pusta lista bez zmian nie tworzy kroku. */
export const clearAnnotation = (history: AnnotationHistory): AnnotationHistory =>
  (history.present.length === 0 ? history : pushAnnotationState(history, []));

export const undoAnnotation = (history: AnnotationHistory): AnnotationHistory => {
  if (history.past.length === 0) return history;
  return {
    past: history.past.slice(0, -1),
    present: history.past[history.past.length - 1],
    future: [history.present, ...history.future],
  };
};

export const redoAnnotation = (history: AnnotationHistory): AnnotationHistory => {
  if (history.future.length === 0) return history;
  return { past: [...history.past, history.present], present: history.future[0], future: history.future.slice(1) };
};

/** Czy aktualny rysunek rozni sie od zapisanego (niezapisane zmiany). */
export const hasUnsavedAnnotationChanges = (history: AnnotationHistory, savedElements: AnnotationElement[]) =>
  JSON.stringify(history.present) !== JSON.stringify(savedElements);

import type { ProjectSummary } from './projectSummaryDataTypes';

// Kafelki podsumowania projektu - czysta funkcja (testowana przez node --test), bez Reacta.

export interface ProjectSummaryStatistic {
  label: string;
  value: number;
}

/** Kolejnosc kafelkow: drzewa, lokalizacja, zgromadzony material. Etykiety po polsku, jak w nawigacji. */
export const toProjectSummaryStatistics = (projectSummary: ProjectSummary): ProjectSummaryStatistic[] => [
  { label: 'Drzewa', value: projectSummary.treeCount },
  { label: 'Z lokalizacją', value: projectSummary.treeWithLocationCount },
  { label: 'Bez lokalizacji', value: projectSummary.treeWithoutLocationCount },
  { label: 'Zdjęcia', value: projectSummary.photoCount },
  { label: 'Zalecenia i prace', value: projectSummary.workCount },
];

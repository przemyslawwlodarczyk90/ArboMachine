import { sendApiRequestForBlob } from '../../../api/httpClient';

/** Etap 54: ktore drzewa dostaja w PDF pelna karte (backend: TreeCardScope). */
export type TreeCardScope = 'ALL' | 'WITH_OPEN_WORKS' | 'NONE';

export const TREE_CARD_SCOPE_OPTIONS: { scope: TreeCardScope; label: string }[] = [
  { scope: 'ALL', label: 'Wszystkie drzewa' },
  { scope: 'WITH_OPEN_WORKS', label: 'Tylko drzewa z pracami do wykonania' },
  { scope: 'NONE', label: 'Bez kart drzew (same zestawienia)' },
];

/** Dokumenty projektu generowane na zadanie z aktualnych danych (backend: domain.projectdocument). */
export const projectDocumentsApi = {
  /** "Dokument projektu" (PDF, wersja MVP); etap 54: zakres kart drzew. */
  downloadProjectReport: (projectId: number, treeCardScope: TreeCardScope = 'ALL') =>
    sendApiRequestForBlob(`/api/projects/${projectId}/documents/project-report?treeCards=${treeCardScope}`, { sendAccessToken: true }),
};

/** Nazwa pobieranego pliku - jak w Content-Disposition backendu (tylko id projektu, bez nazwy od uzytkownika). */
export const projectReportFileName = (projectId: number) => `arbomachine-projekt-${projectId}.pdf`;

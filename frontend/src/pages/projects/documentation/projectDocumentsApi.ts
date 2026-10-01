import { sendApiRequestForBlob } from '../../../api/httpClient';

/** Dokumenty projektu generowane na zadanie z aktualnych danych (backend: domain.projectdocument). */
export const projectDocumentsApi = {
  /** "Dokument projektu" (PDF, wersja MVP). */
  downloadProjectReport: (projectId: number) =>
    sendApiRequestForBlob(`/api/projects/${projectId}/documents/project-report`, { sendAccessToken: true }),
};

/** Nazwa pobieranego pliku - jak w Content-Disposition backendu (tylko id projektu, bez nazwy od uzytkownika). */
export const projectReportFileName = (projectId: number) => `arbomachine-projekt-${projectId}.pdf`;

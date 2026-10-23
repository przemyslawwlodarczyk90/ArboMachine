import { sendApiRequest } from '../../api/httpClient';
import type { DocumentAuthorProfile, DocumentAuthorProfileRequest } from './documentAuthorProfileFormValues';

/** Dane autora opracowania zalogowanego uzytkownika (/api/users/me/document-author-profile) - tylko online. */
export const documentAuthorProfileApi = {
  getDocumentAuthorProfile: () =>
    sendApiRequest<DocumentAuthorProfile>('/api/users/me/document-author-profile', { sendAccessToken: true }),

  /** Zastepuje calosc; pusty request usuwa dane (dokument pokaze "Dane do uzupełnienia"). */
  saveDocumentAuthorProfile: (request: DocumentAuthorProfileRequest) =>
    sendApiRequest<DocumentAuthorProfile>('/api/users/me/document-author-profile', {
      httpMethod: 'PUT', sendAccessToken: true, requestBody: request,
    }),
};

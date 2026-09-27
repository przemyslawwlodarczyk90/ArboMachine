import { sendApiRequest } from './httpClient';
import type { UserAccount, UserProfile } from './apiDataTypes';

/** Wlasne konto zalogowanego uzytkownika (/api/users/me) - dostepne dla kazdej roli. */
export const currentUserAccountApi = {
  getCurrentUserAccount: () =>
    sendApiRequest<UserAccount>('/api/users/me', { sendAccessToken: true }),

  updateCurrentUserProfile: (updatedProfile: UserProfile) =>
    sendApiRequest<UserAccount>('/api/users/me', { httpMethod: 'PUT', requestBody: updatedProfile, sendAccessToken: true }),

  deleteCurrentUserAccount: () =>
    sendApiRequest<void>('/api/users/me', { httpMethod: 'DELETE', sendAccessToken: true }),
};

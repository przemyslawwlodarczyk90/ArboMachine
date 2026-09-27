import { sendApiRequest } from '../../api/httpClient';
import type { AssignableUserRole, UserAccount, UserProfile, UserRole } from '../../api/apiDataTypes';

/** Zarzadzanie cudzymi kontami (/api/admin/users/**) - tylko dla roli ADMIN. */
export const adminUserAccountsApi = {
  listUserAccounts: (roleFilter?: UserRole) =>
    sendApiRequest<UserAccount[]>(`/api/admin/users${roleFilter ? `?role=${roleFilter}` : ''}`, { sendAccessToken: true }),

  updateUserProfile: (userAccountId: number, updatedProfile: UserProfile) =>
    sendApiRequest<UserAccount>(`/api/admin/users/${userAccountId}`, {
      httpMethod: 'PUT', requestBody: updatedProfile, sendAccessToken: true,
    }),

  changeUserRole: (userAccountId: number, newRole: AssignableUserRole) =>
    sendApiRequest<UserAccount>(`/api/admin/users/${userAccountId}/role`, {
      httpMethod: 'PATCH', requestBody: { newRole }, sendAccessToken: true,
    }),

  setAccountLockedByAdmin: (userAccountId: number, lockedByAdmin: boolean) =>
    sendApiRequest<UserAccount>(`/api/admin/users/${userAccountId}/locked`, {
      httpMethod: 'PATCH', requestBody: { lockedByAdmin }, sendAccessToken: true,
    }),

  confirmEmailOnUsersBehalf: (userAccountId: number) =>
    sendApiRequest<UserAccount>(`/api/admin/users/${userAccountId}/confirm-email`, {
      httpMethod: 'POST', sendAccessToken: true,
    }),

  deleteUserAccount: (userAccountId: number) =>
    sendApiRequest<void>(`/api/admin/users/${userAccountId}`, { httpMethod: 'DELETE', sendAccessToken: true }),
};

import { sendApiRequest } from '../../api/httpClient';
import type { RolePanelWelcome } from '../../api/apiDataTypes';
import type { UserAccountStatistics } from './adminDataTypes';

/** Pulpit admina: powitanie panelu i statystyki kont. */
export const adminPanelApi = {
  getAdminPanelWelcome: () =>
    sendApiRequest<RolePanelWelcome>('/api/admin/panel', { sendAccessToken: true }),

  getUserAccountStatistics: () =>
    sendApiRequest<UserAccountStatistics>('/api/admin/stats', { sendAccessToken: true }),
};

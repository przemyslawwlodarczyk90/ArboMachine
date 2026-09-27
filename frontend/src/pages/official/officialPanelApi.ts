import { sendApiRequest } from '../../api/httpClient';
import type { RolePanelWelcome } from '../../api/apiDataTypes';

/** Endpointy modulu urzednika (/api/official/**). Nowe funkcje panelu urzednika dopisujemy tutaj. */
export const officialPanelApi = {
  getOfficialPanelWelcome: () =>
    sendApiRequest<RolePanelWelcome>('/api/official/panel', { sendAccessToken: true }),
};

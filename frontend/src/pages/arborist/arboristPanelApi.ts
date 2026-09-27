import { sendApiRequest } from '../../api/httpClient';
import type { RolePanelWelcome } from '../../api/apiDataTypes';

/** Endpointy modulu arborysty (/api/arborist/**). Nowe funkcje panelu arborysty dopisujemy tutaj. */
export const arboristPanelApi = {
  getArboristPanelWelcome: () =>
    sendApiRequest<RolePanelWelcome>('/api/arborist/panel', { sendAccessToken: true }),
};

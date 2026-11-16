import { sendApiRequest } from '../api/httpClient';

export interface DemoModeStatus {
  enabled: boolean;
}

export interface DemoSessionResponse {
  accessToken: string;
  validForSeconds: number;
}

/** Publiczne endpointy trybu demo (bez logowania). */
export const demoApi = {
  getStatus: () => sendApiRequest<DemoModeStatus>('/api/demo'),
  startSession: () => sendApiRequest<DemoSessionResponse>('/api/demo/session', { httpMethod: 'POST' }),
};

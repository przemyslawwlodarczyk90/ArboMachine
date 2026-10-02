import { sendApiRequest } from '../api/httpClient';

// Powiadomienia wlasnego konta (etapy 29-30, backend: domain.notification).

export type UserNotificationType = 'TREE_POOR_HEALTH' | 'WORKS_DUE_TOMORROW' | 'TEST';

export interface UserNotification {
  id: number;
  type: UserNotificationType;
  title: string;
  body: string;
  /** trasa aplikacji do otwarcia (np. /projects/1/trees/2) albo null */
  linkPath: string | null;
  createdAt: string;
}

/** Wynik PushSubscription.toJSON() przegladarki. */
export interface BrowserPushSubscription {
  endpoint: string;
  expirationTime?: number | null;
  keys: { p256dh: string; auth: string };
}

const jsonBody = (body: unknown) => ({ httpMethod: 'POST' as const, requestBody: body, sendAccessToken: true });

export const notificationsApi = {
  listNotifications: () => sendApiRequest<UserNotification[]>('/api/notifications', { sendAccessToken: true }),

  getPushPublicKey: () => sendApiRequest<{ publicKey: string }>('/api/notifications/push/config', { sendAccessToken: true }),

  saveSubscription: (subscription: BrowserPushSubscription) =>
    sendApiRequest<void>('/api/notifications/push/subscription', { httpMethod: 'PUT', requestBody: subscription, sendAccessToken: true }),

  removeSubscription: (endpoint: string) => sendApiRequest<void>('/api/notifications/push/subscription/removal', jsonBody({ endpoint })),

  isSubscribed: (endpoint: string) =>
    sendApiRequest<{ subscribed: boolean }>('/api/notifications/push/subscription/status', jsonBody({ endpoint })),

  sendTestNotification: () => sendApiRequest<{ deliveredDeviceCount: number }>('/api/notifications/test', jsonBody(undefined)),
};

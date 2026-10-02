import { notificationsApi, type BrowserPushSubscription } from './notificationsApi';
import { base64UrlToBytes, type PushAvailability } from './pushSupport';

// Web Push w przegladarce: service worker (public/sw.js, rejestrowany w zbudowanej aplikacji) + PushManager.
// Subskrypcja jest per urzadzenie (przegladarka); backend przypisuje ja do zalogowanego konta.

export function pushAvailability(): PushAvailability {
  if (typeof window === 'undefined' || !('Notification' in window) || !('PushManager' in window)) return 'UNSUPPORTED_BROWSER';
  if (!window.isSecureContext) return 'INSECURE_CONTEXT';
  if (!('serviceWorker' in navigator) || !import.meta.env.PROD) return 'NO_SERVICE_WORKER';
  if (Notification.permission === 'denied') return 'BLOCKED';
  return 'AVAILABLE';
}

async function currentBrowserSubscription(): Promise<PushSubscription | null> {
  const registration = await navigator.serviceWorker.ready;
  return registration.pushManager.getSubscription();
}

/** Czy to urzadzenie dostaje powiadomienia ZALOGOWANEGO konta (subskrypcja w przegladarce i na serwerze). */
export async function isThisDeviceSubscribed(): Promise<boolean> {
  if (pushAvailability() !== 'AVAILABLE') return false;
  const subscription = await currentBrowserSubscription();
  if (!subscription) return false;
  return (await notificationsApi.isSubscribed(subscription.endpoint)).subscribed;
}

/** Wlaczenie: zgoda uzytkownika -> subskrypcja przegladarki z kluczem VAPID serwera -> zapis na serwerze. */
export async function subscribeThisDevice(): Promise<'SUBSCRIBED' | 'PERMISSION_DENIED'> {
  const permission = await Notification.requestPermission();
  if (permission !== 'granted') return 'PERMISSION_DENIED';
  const registration = await navigator.serviceWorker.ready;
  const { publicKey } = await notificationsApi.getPushPublicKey();
  const subscription = await registration.pushManager.getSubscription()
    ?? await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: base64UrlToBytes(publicKey) });
  await notificationsApi.saveSubscription(subscription.toJSON() as BrowserPushSubscription);
  return 'SUBSCRIBED';
}

/** Wylaczenie: usuniecie z serwera i rezygnacja z subskrypcji w przegladarce. */
export async function unsubscribeThisDevice(): Promise<void> {
  const subscription = await currentBrowserSubscription();
  if (!subscription) return;
  await notificationsApi.removeSubscription(subscription.endpoint);
  await subscription.unsubscribe();
}

// Logika Web Push bez Reacta i bez API przegladarki - testowana przez node --test.

/** Klucz VAPID serwera (base64url) -> bajty dla PushManager.subscribe({ applicationServerKey }). */
export function base64UrlToBytes(base64Url: string): Uint8Array<ArrayBuffer> {
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(base64Url.length / 4) * 4, '=');
  const binary = atob(base64);
  const bytes = new Uint8Array(new ArrayBuffer(binary.length));
  for (let index = 0; index < binary.length; index++) bytes[index] = binary.charCodeAt(index);
  return bytes;
}

/** Dlaczego push nie dziala na tym urzadzeniu - polski komunikat albo null (dziala). */
export type PushAvailability = 'AVAILABLE' | 'UNSUPPORTED_BROWSER' | 'INSECURE_CONTEXT' | 'NO_SERVICE_WORKER' | 'BLOCKED';

export function pushAvailabilityMessage(availability: PushAvailability): string | null {
  switch (availability) {
    case 'AVAILABLE': return null;
    case 'UNSUPPORTED_BROWSER': return 'Ta przeglądarka nie obsługuje powiadomień push. Na iPhonie/iPadzie dodaj aplikację do ekranu początkowego.';
    case 'INSECURE_CONTEXT': return 'Powiadomienia push wymagają bezpiecznego połączenia (HTTPS).';
    case 'NO_SERVICE_WORKER': return 'Powiadomienia push działają w zbudowanej aplikacji (nie w trybie deweloperskim).';
    case 'BLOCKED': return 'Powiadomienia są zablokowane w ustawieniach przeglądarki dla tej strony - odblokuj je, aby włączyć.';
  }
}

/** Polskie nazwy rodzajow powiadomien na liscie. */
export const NOTIFICATION_TYPE_LABELS: Record<string, string> = {
  TREE_POOR_HEALTH: 'Stan drzewa',
  WORKS_DUE_TOMORROW: 'Przypomnienie o pracach',
  TEST: 'Test',
};

/**
 * Data i godzina z backendu (ISO-8601 w UTC) w lokalnej strefie czasowej przegladarki.
 * Format pl-PL (np. "27 wrz 2026, 19:30") - zgodny z polskim UI panelu arborysty (jedyny uzytkownik funkcji),
 * niezaleznie od jezyka przegladarki.
 */
export function formatTimestampForDisplay(isoTimestamp: string): string {
  return new Date(isoTimestamp).toLocaleString('pl-PL', { dateStyle: 'medium', timeStyle: 'short' });
}

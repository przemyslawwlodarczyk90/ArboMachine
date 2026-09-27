/**
 * Data i godzina z backendu (ISO-8601 w UTC) w lokalnej strefie czasowej przegladarki.
 * Format en-GB (np. "27 Sept 2026, 19:30") - zgodny z angielskim UI, niezaleznie od jezyka przegladarki.
 */
export function formatTimestampForDisplay(isoTimestamp: string): string {
  return new Date(isoTimestamp).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' });
}

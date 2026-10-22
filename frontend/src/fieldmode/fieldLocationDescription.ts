// Uczciwy opis lokalizacji w trybie terenowym (etap 32). Przegladarka nie wie, czy pozycja pochodzi z GPS tabletu,
// czy z odbiornika RTK podstawionego jako pozorowana lokalizacja, i nie zna typu rozwiazania (FIX/FLOAT) - dlatego
// tu nigdy nie piszemy "RTK", tylko zrodlo, dokladnosc ZGLOSZONA przez urzadzenie i czas pomiaru.
// Etykiety widoku biurowego (deviceGnss.describeLocationMeasurement) zostaja bez zmian. Modul bez React (node --test).
import { formatAccuracy } from '../gnss/deviceGnss.ts';

const FIELD_SOURCE_LABELS: Record<string, string> = {
  DEVICE_GNSS: 'GPS urządzenia (przeglądarka)',
  MAP: 'Wskazane na mapie',
  MANUAL: 'Wpisane ręcznie',
  IMPORT: 'Import',
};

export const fieldLocationSourceLabel = (source: string | null | undefined): string =>
  (source && FIELD_SOURCE_LABELS[source]) || 'Źródło nieznane (punkt sprzed zapisu źródła)';

export const formatReportedAccuracy = (accuracyM: number) => `${formatAccuracy(accuracyM)} (dokładność zgłoszona przez urządzenie)`;

/** Godzina pomiaru (czas lokalny urzadzenia), np. "10:31:22". Od etapu 35 zapisywana takze przy punkcie drzewa. */
export function formatMeasurementTime(measuredAt: string, timeZone?: string): string {
  return new Date(measuredAt).toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit', second: '2-digit', timeZone });
}

/** Opis zapisanego punktu w panelu drzewa: zrodlo, dokladnosc zgloszona przez urzadzenie, wysokosc. */
export function describeFieldLocation(location: {
  accuracyM?: number | null; altitudeM?: number | null; source?: string | null; measuredAt?: string | null;
}, timeZone?: string): string {
  const parts = [fieldLocationSourceLabel(location.source)];
  if (location.accuracyM !== null && location.accuracyM !== undefined) parts.push(formatReportedAccuracy(location.accuracyM));
  if (location.altitudeM !== null && location.altitudeM !== undefined) parts.push(`wysokość ${Math.round(location.altitudeM)} m n.p.m.`);
  // Etap 35: czas pomiaru zapisany przy punkcie (wczesniej znany tylko przed zapisem).
  if (location.measuredAt) parts.push(`pomiar o ${formatMeasurementTime(location.measuredAt, timeZone)}`);
  return parts.join(' · ');
}

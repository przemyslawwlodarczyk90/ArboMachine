// GNSS urzadzenia (etap 25) przez przegladarkowe Geolocation API: GPS tabletu/telefonu, a gdy go brak - lokalizacja
// z Wi-Fi/sieci (znacznie mniej dokladna). Modul bez importow wartosci i bez DOM - testowany przez node --test
// z podstawionym obiektem geolocation. Zewnetrzne odbiorniki (Bluetooth) i RTK/NTRIP - poza V1 (DO_DOPRECYZOWANIA).

/** Minimalny interfejs navigator.geolocation potrzebny do jednego pomiaru (do podstawienia w testach). */
export interface GeolocationLike {
  getCurrentPosition(
    onSuccess: (position: { coords: GeolocationCoordinatesLike; timestamp: number }) => void,
    onError: (error: { code: number }) => void,
    options?: { enableHighAccuracy?: boolean; timeout?: number; maximumAge?: number },
  ): void;
}

export interface GeolocationCoordinatesLike {
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude: number | null;
}

/** Wynik pomiaru: wspolrzedne WGS84, dokladnosc pozioma (m, ok. 95% ufnosci), wysokosc n.p.m. (m) albo null. */
export interface GnssReading {
  latitude: number;
  longitude: number;
  accuracyM: number;
  altitudeM: number | null;
  measuredAt: string;
}

export type GnssFailureReason = 'UNSUPPORTED' | 'INSECURE_CONTEXT' | 'PERMISSION_DENIED' | 'POSITION_UNAVAILABLE' | 'TIMEOUT';

export class GnssReadingError extends Error {
  readonly reason: GnssFailureReason;

  constructor(reason: GnssFailureReason) {
    super(describeGnssFailure(reason));
    this.reason = reason;
  }
}

/** V1: najdokladniejszy pomiar (GPS), maks. 30 s czekania, bez wyniku z pamieci podrecznej przegladarki. */
export const GNSS_READING_OPTIONS = { enableHighAccuracy: true, timeout: 30_000, maximumAge: 0 } as const;

/** Kody bledow GeolocationPositionError (stale z MDN). */
const PERMISSION_DENIED = 1;
const POSITION_UNAVAILABLE = 2;
const TIMEOUT = 3;

export function describeGnssFailure(reason: GnssFailureReason): string {
  switch (reason) {
    case 'UNSUPPORTED':
      return 'Ta przeglądarka nie udostępnia lokalizacji urządzenia.';
    case 'INSECURE_CONTEXT':
      return 'Lokalizacja urządzenia działa tylko przez bezpieczne połączenie (HTTPS).';
    case 'PERMISSION_DENIED':
      return 'Nie uzyskano lokalizacji - brak zgody na dostęp do lokalizacji. Zezwól na nią w ustawieniach przeglądarki.';
    case 'POSITION_UNAVAILABLE':
      return 'Nie uzyskano lokalizacji - urządzenie nie ustaliło pozycji. Sprawdź, czy lokalizacja (GPS) jest włączona, i spróbuj na otwartym terenie.';
    case 'TIMEOUT':
      return 'Nie uzyskano lokalizacji w ciągu 30 sekund. Spróbuj ponownie, najlepiej z dala od budynków i gęstych koron.';
  }
}

/** Zamienia wynik Geolocation API na odczyt z zaokragleniem do precyzji zapisu backendu (7 / 2 miejsca). */
export function toGnssReading(position: { coords: GeolocationCoordinatesLike; timestamp: number }): GnssReading {
  const round = (value: number, decimalPlaces: number) => Number(value.toFixed(decimalPlaces));
  return {
    latitude: round(position.coords.latitude, 7),
    longitude: round(position.coords.longitude, 7),
    accuracyM: round(Math.min(position.coords.accuracy, 10_000), 2),
    altitudeM: position.coords.altitude === null || position.coords.altitude === undefined
      ? null : round(position.coords.altitude, 2),
    measuredAt: new Date(position.timestamp).toISOString(),
  };
}

/** Jeden pomiar. geolocation null = przegladarka bez Geolocation API; isSecureContext false = strona po http. */
export function readDeviceGnssPosition(geolocation: GeolocationLike | null | undefined, isSecureContext: boolean): Promise<GnssReading> {
  if (!isSecureContext) return Promise.reject(new GnssReadingError('INSECURE_CONTEXT'));
  if (!geolocation) return Promise.reject(new GnssReadingError('UNSUPPORTED'));
  return new Promise((resolve, reject) => {
    geolocation.getCurrentPosition(
      (position) => resolve(toGnssReading(position)),
      (error) => reject(new GnssReadingError(error.code === PERMISSION_DENIED ? 'PERMISSION_DENIED'
        : error.code === TIMEOUT ? 'TIMEOUT'
          : error.code === POSITION_UNAVAILABLE ? 'POSITION_UNAVAILABLE' : 'POSITION_UNAVAILABLE')),
      GNSS_READING_OPTIONS,
    );
  });
}

export type GnssAccuracyQuality = 'GOOD' | 'ACCEPTABLE' | 'POOR';

/**
 * Ocena dokladnosci V1 (do potwierdzenia z arborysta): do 5 m - dobra (GPS na otwartym terenie), do 15 m -
 * akceptowalna, powyzej - slaba (zwykle lokalizacja z Wi-Fi/sieci albo zaslonieta antena).
 */
export const GNSS_ACCURACY_THRESHOLDS_M = { good: 5, acceptable: 15 } as const;

export function gnssAccuracyQuality(accuracyM: number): GnssAccuracyQuality {
  if (accuracyM <= GNSS_ACCURACY_THRESHOLDS_M.good) return 'GOOD';
  if (accuracyM <= GNSS_ACCURACY_THRESHOLDS_M.acceptable) return 'ACCEPTABLE';
  return 'POOR';
}

/** "±3,5 m" - dokladnosc po polsku. */
export const formatAccuracy = (accuracyM: number) =>
  `±${(Math.round(accuracyM * 10) / 10).toString().replace('.', ',')} m`;

const SOURCE_LABELS: Record<string, string> = {
  MANUAL: 'wpisane ręcznie',
  MAP: 'wskazane na mapie',
  DEVICE_GNSS: 'GPS urządzenia',
  IMPORT: 'import z pliku',
};

/** Opis pomiaru zapisanej lokalizacji, np. "GPS urządzenia, dokładność ±4 m, wysokość 104 m n.p.m."; null = brak danych. */
export function describeLocationMeasurement(location: {
  accuracyM?: number | null; altitudeM?: number | null; source?: string | null;
}): string | null {
  const parts: string[] = [];
  if (location.source && SOURCE_LABELS[location.source]) parts.push(SOURCE_LABELS[location.source]);
  if (location.accuracyM !== null && location.accuracyM !== undefined) parts.push(`dokładność ${formatAccuracy(location.accuracyM)}`);
  if (location.altitudeM !== null && location.altitudeM !== undefined) {
    parts.push(`wysokość ${Math.round(location.altitudeM).toString()} m n.p.m.`);
  }
  if (parts.length === 0) return null;
  const text = parts.join(', ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

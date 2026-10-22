// GNSS urzadzenia (etap 25) przez przegladarkowe Geolocation API: GPS tabletu/telefonu, a gdy go brak - lokalizacja
// z Wi-Fi/sieci (znacznie mniej dokladna). Modul bez importow wartosci i bez DOM - testowany przez node --test
// z podstawionym obiektem geolocation. Zewnetrzny odbiornik RTK (etap 31): bez wlasnej obslugi NMEA/Bluetooth -
// aplikacja odbiornika na Androidzie podstawia pozycje jako lokalizacje systemu (mock location), a my czytamy ja tym
// samym Geolocation API. Typ rozwiazania (FIX/FLOAT), PDOP i liczba satelitow nie sa dostepne (DO_DOPRECYZOWANIA).

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

export type GnssAccuracyQuality = 'SURVEY' | 'SUBMETER' | 'GOOD' | 'ACCEPTABLE' | 'POOR';

/**
 * Ocena dokladnosci V1 (do potwierdzenia z arborysta): do 10 cm - geodezyjna (odbiornik RTK z korektami), do 1 m -
 * submetrowa (odbiornik bez RTK / RTK FLOAT), do 5 m - dobra (GPS na otwartym terenie), do 15 m - akceptowalna,
 * powyzej - slaba (zwykle lokalizacja z Wi-Fi/sieci albo zaslonieta antena).
 */
export const GNSS_ACCURACY_THRESHOLDS_M = { survey: 0.1, submeter: 1, good: 5, acceptable: 15 } as const;

export function gnssAccuracyQuality(accuracyM: number): GnssAccuracyQuality {
  if (accuracyM <= GNSS_ACCURACY_THRESHOLDS_M.survey) return 'SURVEY';
  if (accuracyM <= GNSS_ACCURACY_THRESHOLDS_M.submeter) return 'SUBMETER';
  if (accuracyM <= GNSS_ACCURACY_THRESHOLDS_M.good) return 'GOOD';
  if (accuracyM <= GNSS_ACCURACY_THRESHOLDS_M.acceptable) return 'ACCEPTABLE';
  return 'POOR';
}

/** "±3,5 m" - dokladnosc po polsku; ponizej metra w centymetrach ("±2 cm"), bo tak mowi sie o RTK. */
export const formatAccuracy = (accuracyM: number) => (accuracyM < 1
  ? `±${Math.round(accuracyM * 100)} cm`
  : `±${(Math.round(accuracyM * 10) / 10).toString().replace('.', ',')} m`);

/** Interfejs navigator.geolocation do serii pomiarow (watchPosition) - do podstawienia w testach. */
export interface GeolocationWatchLike {
  watchPosition(
    onSuccess: (position: { coords: GeolocationCoordinatesLike; timestamp: number }) => void,
    onError: (error: { code: number }) => void,
    options?: { enableHighAccuracy?: boolean; timeout?: number; maximumAge?: number },
  ): number;
  clearWatch(watchId: number): void;
}

/** Pomiar usredniony: srednia z kilku odczytow + najwieksza odleglosc odczytu od sredniej (rozrzut). */
export interface AveragedGnssReading extends GnssReading {
  sampleCount: number;
  spreadM: number;
}

/** V1: 10 odczytow albo 20 s - co nastapi pierwsze (do potwierdzenia, ile trwa pomiar przy drzewie u arborysty). */
export const GNSS_AVERAGING = { sampleCount: 10, maximumDurationMs: 20_000 } as const;

const METERS_PER_DEGREE_LATITUDE = 111_320;

/**
 * Srednia z odczytow (stopnie - na obszarze kilku metrow srednia arytmetyczna wystarcza). Dokladnosc = srednia
 * zglaszanych dokladnosci, ale nie lepsza niz rozrzut - nie udajemy wiekszej precyzji niz pokazuja same odczyty.
 */
export function averageGnssReadings(readings: GnssReading[]): AveragedGnssReading {
  if (readings.length === 0) throw new Error('No readings to average');
  const mean = (values: number[]) => values.reduce((sum, value) => sum + value, 0) / values.length;
  const latitude = mean(readings.map((reading) => reading.latitude));
  const longitude = mean(readings.map((reading) => reading.longitude));
  const metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * Math.cos(latitude * Math.PI / 180);
  const spreadM = Math.max(...readings.map((reading) => Math.hypot(
    (reading.latitude - latitude) * METERS_PER_DEGREE_LATITUDE, (reading.longitude - longitude) * metersPerDegreeLongitude)));
  const altitudes = readings.map((reading) => reading.altitudeM).filter((altitude): altitude is number => altitude !== null);
  const round = (value: number, decimalPlaces: number) => Number(value.toFixed(decimalPlaces));
  return {
    latitude: round(latitude, 7),
    longitude: round(longitude, 7),
    accuracyM: round(Math.max(mean(readings.map((reading) => reading.accuracyM)), spreadM), 2),
    altitudeM: altitudes.length ? round(mean(altitudes), 2) : null,
    measuredAt: readings[readings.length - 1].measuredAt,
    sampleCount: readings.length,
    spreadM: round(spreadM, 2),
  };
}

/**
 * Seria odczytow (watchPosition) do usrednienia: konczy sie po GNSS_AVERAGING.sampleCount odczytach albo po czasie
 * (wtedy usrednia to, co jest; zero odczytow = TIMEOUT). onProgress - liczba odczytow do paska postepu.
 * abort() (np. "Anuluj") zatrzymuje serie - przerwana seria nie konczy obietnicy.
 */
export function collectAveragedGnssReading(geolocation: GeolocationWatchLike | null | undefined, isSecureContext: boolean,
                                           onProgress: (sampleCount: number) => void,
                                           schedule: (callback: () => void, delayMs: number) => unknown = setTimeout,
): { result: Promise<AveragedGnssReading>; abort: () => void } {
  let abort = () => {};
  const result = new Promise<AveragedGnssReading>((resolve, reject) => {
    if (!isSecureContext) { reject(new GnssReadingError('INSECURE_CONTEXT')); return; }
    if (!geolocation) { reject(new GnssReadingError('UNSUPPORTED')); return; }
    const readings: GnssReading[] = [];
    let isFinished = false;
    let watchId: number | null = null;
    const finish = (error?: GnssReadingError) => {
      if (isFinished) return;
      isFinished = true;
      if (watchId !== null) geolocation.clearWatch(watchId);
      if (error) reject(error);
      else if (readings.length === 0) reject(new GnssReadingError('TIMEOUT'));
      else resolve(averageGnssReadings(readings));
    };
    watchId = geolocation.watchPosition(
      (position) => {
        if (isFinished) return;
        readings.push(toGnssReading(position));
        onProgress(readings.length);
        if (readings.length >= GNSS_AVERAGING.sampleCount) finish();
      },
      (error) => {
        // Pojedynczy blad w serii nie przerywa pomiaru, jesli sa juz odczyty; brak zgody - zawsze.
        if (error.code === PERMISSION_DENIED) finish(new GnssReadingError('PERMISSION_DENIED'));
        else if (readings.length === 0 && error.code !== TIMEOUT) finish(new GnssReadingError('POSITION_UNAVAILABLE'));
      },
      GNSS_READING_OPTIONS,
    );
    // Odczyty moga przyjsc synchronicznie (testy) - wtedy seria mogla sie juz skonczyc przed przypisaniem watchId.
    if (isFinished) geolocation.clearWatch(watchId);
    schedule(() => finish(), GNSS_AVERAGING.maximumDurationMs);
    abort = () => {
      if (isFinished) return;
      isFinished = true;
      if (watchId !== null) geolocation.clearWatch(watchId);
    };
  });
  return { result, abort: () => abort() };
}

const SOURCE_LABELS: Record<string, string> = {
  MANUAL: 'wpisane ręcznie',
  MAP: 'wskazane na mapie',
  DEVICE_GNSS: 'GPS urządzenia',
  IMPORT: 'import z pliku',
};

/**
 * Opis pomiaru zapisanej lokalizacji, np. "GPS urządzenia, dokładność ±4 m, wysokość 104 m n.p.m., pomiar 22.10.2026
 * 10:31"; null = brak danych. Etap 35: czas pomiaru (czas lokalny urzadzenia), gdy zostal zapisany.
 */
export function describeLocationMeasurement(location: {
  accuracyM?: number | null; altitudeM?: number | null; source?: string | null; measuredAt?: string | null;
}, timeZone?: string): string | null {
  const parts: string[] = [];
  if (location.source && SOURCE_LABELS[location.source]) parts.push(SOURCE_LABELS[location.source]);
  if (location.accuracyM !== null && location.accuracyM !== undefined) parts.push(`dokładność ${formatAccuracy(location.accuracyM)}`);
  if (location.altitudeM !== null && location.altitudeM !== undefined) {
    parts.push(`wysokość ${Math.round(location.altitudeM).toString()} m n.p.m.`);
  }
  if (location.measuredAt) {
    parts.push(`pomiar ${new Date(location.measuredAt).toLocaleString('pl-PL', {
      day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', timeZone })}`);
  }
  if (parts.length === 0) return null;
  const text = parts.join(', ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

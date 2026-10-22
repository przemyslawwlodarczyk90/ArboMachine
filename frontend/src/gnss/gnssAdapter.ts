// Etap 35: wspolny interfejs zrodel pozycji GNSS. Ekrany (pomiar drzewa, "+ Drzewo tutaj") nie wolaja juz bezposrednio
// Geolocation API, tylko adaptera - kolejny (aplikacja natywna / Bluetooth / SDK producenta odbiornika) dojdzie bez
// zmian w ekranach, gdy poznamy sprzet klienta. Modul bez React i bez DOM - testowany przez node --test.
import {
  collectAveragedGnssReading, readDeviceGnssPosition, type AveragedGnssReading, type GeolocationLike,
  type GeolocationWatchLike, type GnssReading,
} from './deviceGnss.ts';

/** Jeden odczyt albo seria usredniona. */
export type GnssMeasurementMode = 'SINGLE' | 'AVERAGED';

/**
 * Typ rozwiazania odbiornika. Ustawia go WYLACZNIE adapter, ktory naprawde dostal te informacje ze sprzetu
 * (NMEA / SDK) - nigdy nie wyliczamy "FIX" z samej dokladnosci. Adapter przegladarki go nie zna (undefined).
 */
export type GnssSolutionType = 'FIX' | 'FLOAT' | 'DGPS' | 'SPS' | 'UNKNOWN';

/** Wynik pomiaru z adaptera: odczyt + skad pochodzi. */
export type GnssFix = (GnssReading | AveragedGnssReading) & {
  /** kod sposobu pomiaru zapisywany przy lokalizacji drzewa (TreeLocation.adapterCode w backendzie) */
  adapterCode: string;
  /** opis urzadzenia, jesli adapter go zna (np. model odbiornika); przegladarka - null */
  deviceLabel: string | null;
  solutionType?: GnssSolutionType;
};

export interface GnssMeasurement {
  result: Promise<GnssFix>;
  /** przerwanie pomiaru (np. "Anuluj") - przerwany pomiar nie konczy obietnicy */
  abort: () => void;
}

export interface GnssAdapter {
  readonly code: string;
  /** nazwa dla uzytkownika */
  readonly label: string;
  isAvailable(): boolean;
  measure(mode: GnssMeasurementMode, onProgress: (sampleCount: number) => void): GnssMeasurement;
}

export const BROWSER_GEOLOCATION_ADAPTER_CODE = 'browser-geolocation';
export const BROWSER_GEOLOCATION_AVERAGED_CODE = 'browser-geolocation-averaged';

/**
 * Adapter przegladarki (Geolocation API): GPS tabletu albo odbiornik podstawiony przez jego aplikacje jako
 * "pozorowana lokalizacja" Androida. Nie wie, ktore z nich - i nie zna typu rozwiazania RTK.
 */
export function createBrowserGeolocationAdapter(
  geolocation: (GeolocationLike & GeolocationWatchLike) | null | undefined,
  isSecureContext: boolean,
): GnssAdapter {
  return {
    code: BROWSER_GEOLOCATION_ADAPTER_CODE,
    label: 'GPS urządzenia (przeglądarka)',
    isAvailable: () => isSecureContext && !!geolocation,
    measure(mode, onProgress) {
      if (mode === 'AVERAGED') {
        const series = collectAveragedGnssReading(geolocation, isSecureContext, onProgress);
        return {
          result: series.result.then((reading) => ({ ...reading, adapterCode: BROWSER_GEOLOCATION_AVERAGED_CODE, deviceLabel: null })),
          abort: series.abort,
        };
      }
      return {
        result: readDeviceGnssPosition(geolocation, isSecureContext)
          .then((reading) => ({ ...reading, adapterCode: BROWSER_GEOLOCATION_ADAPTER_CODE, deviceLabel: null })),
        abort: () => {},
      };
    },
  };
}

/** Pola lokalizacji drzewa z pomiaru (do PUT .../location i do nowego drzewa). */
export function locationFieldsOfFix(fix: GnssFix) {
  return {
    latitude: fix.latitude,
    longitude: fix.longitude,
    accuracyM: fix.accuracyM,
    ...(fix.altitudeM !== null ? { altitudeM: fix.altitudeM } : {}),
    source: 'DEVICE_GNSS' as const,
    measuredAt: fix.measuredAt,
    adapterCode: fix.adapterCode,
    ...(fix.deviceLabel ? { deviceLabel: fix.deviceLabel } : {}),
  };
}

const ADAPTER_LABELS: Record<string, string> = {
  [BROWSER_GEOLOCATION_ADAPTER_CODE]: 'GPS urządzenia (przeglądarka), jeden odczyt',
  [BROWSER_GEOLOCATION_AVERAGED_CODE]: 'GPS urządzenia (przeglądarka), średnia z serii odczytów',
};

/** Opis sposobu pomiaru zapisanego przy drzewie; nieznany kod - sam kod (np. przyszly adapter odbiornika). */
export const describeAdapterCode = (adapterCode: string | null | undefined): string | null =>
  (adapterCode ? ADAPTER_LABELS[adapterCode] ?? adapterCode : null);

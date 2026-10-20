import type { TreeLocation, TreeLocationRequest } from '../treeDataTypes';
import {
  fromPolishGrid, PL_1992_ZONE, pl2000ZoneByNumber, type PolishGridZone,
} from '../../../map/polishGridCoordinates.ts';
import { MAXIMUM_LATITUDE, MAXIMUM_LONGITUDE, MINIMUM_LATITUDE, MINIMUM_LONGITUDE } from './treeLocationValidRange.ts';

/**
 * Wartosci recznego formularza wspolrzednych - tekst az do wyslania (pozwala wpisac przecinek dziesietny
 * i pokazac czytelny blad zamiast NaN). Dotyczy tylko formularza: mapa tworzy TreeLocationRequest sama (treeLocationMapCoordinates).
 * Od etapu 31 wspolrzedne mozna wpisac takze w PL-2000 (strefy 5-8) albo PL-1992 (np. z operatu geodezyjnego) -
 * formularz przelicza je na WGS 84, bo w tym ukladzie zapisuje backend.
 */
export interface TreeLocationFormValues {
  coordinateSystem: ManualCoordinateSystem;
  latitude: string;
  longitude: string;
  /** X geodezyjne (northing) w metrach - dla PL-2000 / PL-1992 */
  gridX: string;
  /** Y geodezyjne (easting) w metrach - dla PL-2000 / PL-1992 */
  gridY: string;
}

export type ManualCoordinateSystem = 'WGS84' | 'PL_2000_5' | 'PL_2000_6' | 'PL_2000_7' | 'PL_2000_8' | 'PL_1992';

export const MANUAL_COORDINATE_SYSTEMS: { system: ManualCoordinateSystem; label: string }[] = [
  { system: 'WGS84', label: 'WGS 84 (stopnie, jak GPS)' },
  { system: 'PL_2000_5', label: 'PL-2000 strefa 5 (EPSG:2176)' },
  { system: 'PL_2000_6', label: 'PL-2000 strefa 6 (EPSG:2177)' },
  { system: 'PL_2000_7', label: 'PL-2000 strefa 7 (EPSG:2178)' },
  { system: 'PL_2000_8', label: 'PL-2000 strefa 8 (EPSG:2179)' },
  { system: 'PL_1992', label: 'PL-1992 (EPSG:2180)' },
];

/** Granice "rozsadnego" punktu z ukladu krajowego (Polska z zapasem) - poza nimi to prawie na pewno zla strefa / X i Y zamienione. */
const POLAND_WITH_MARGIN = { minimumLatitude: 48.5, maximumLatitude: 55.5, minimumLongitude: 13.5, maximumLongitude: 24.8 };

/** Liczba dziesietna ze znakiem, kropka albo przecinkiem, np. 52.2297, -0,1276. */
const SIGNED_DECIMAL_DEGREES_PATTERN = /^-?\d+([.,]\d+)?$/;
/** Metry ukladu krajowego - liczba dodatnia; spacje tysiecy dozwolone, np. "5 788 456,49". */
const GRID_METERS_PATTERN = /^\d+([.,]\d+)?$/;

export const toTreeLocationFormValues = (location: TreeLocation | null | undefined): TreeLocationFormValues => ({
  coordinateSystem: 'WGS84',
  latitude: location?.latitude.toString() ?? '',
  longitude: location?.longitude.toString() ?? '',
  gridX: '',
  gridY: '',
});

const toNumber = (fieldValue: string) => Number(fieldValue.trim().replace(/[\s ]/g, '').replace(',', '.'));

export function zoneOfCoordinateSystem(system: ManualCoordinateSystem): PolishGridZone | null {
  if (system === 'WGS84') return null;
  if (system === 'PL_1992') return PL_1992_ZONE;
  return pl2000ZoneByNumber(Number(system.slice(-1)));
}

/** Punkt WGS 84 z formularza (po przeliczeniu ukladu krajowego); 7 miejsc po przecinku jak backend. */
export function toWgs84Point(formValues: TreeLocationFormValues): { latitude: number; longitude: number } {
  const zone = zoneOfCoordinateSystem(formValues.coordinateSystem);
  if (!zone) return { latitude: toNumber(formValues.latitude), longitude: toNumber(formValues.longitude) };
  const point = fromPolishGrid({ x: toNumber(formValues.gridX), y: toNumber(formValues.gridY) }, zone);
  return { latitude: Number(point.latitude.toFixed(7)), longitude: Number(point.longitude.toFixed(7)) };
}

/** Reczny formularz: zrodlo MANUAL, bez danych pomiaru (wysokosc/dokladnosc zna tylko GNSS). */
export const toTreeLocationRequest = (formValues: TreeLocationFormValues): TreeLocationRequest => ({
  ...toWgs84Point(formValues),
  source: 'MANUAL',
});

/** Blad jednej wspolrzednej albo undefined, gdy jest poprawna. Obie sa wymagane - brak lokalizacji to "Usun". */
const errorOfCoordinate = (fieldValue: string, coordinateName: string, minimum: number, maximum: number) => {
  const coordinateText = fieldValue.trim();
  if (!coordinateText) return `${coordinateName} jest wymagana.`;
  if (!SIGNED_DECIMAL_DEGREES_PATTERN.test(coordinateText)) {
    return `${coordinateName} musi być liczbą w stopniach dziesiętnych, np. 52.2297.`;
  }
  const degrees = toNumber(coordinateText);
  return degrees >= minimum && degrees <= maximum
    ? undefined
    : `${coordinateName} musi być w zakresie od ${minimum} do ${maximum}.`;
};

const errorOfGridMeters = (fieldValue: string, coordinateName: string) => {
  const text = fieldValue.trim().replace(/[\s ]/g, '');
  if (!text) return `${coordinateName} jest wymagana.`;
  return GRID_METERS_PATTERN.test(text) ? undefined : `${coordinateName} musi być liczbą metrów, np. 5788456,49.`;
};

/** Walidacja w przegladarce - pusty wynik oznacza, ze formularz mozna wyslac. */
export const validateTreeLocationFormValues = (formValues: TreeLocationFormValues): Record<string, string> => {
  const errorMessageByFieldName: Record<string, string> = {};
  if (formValues.coordinateSystem === 'WGS84') {
    const latitudeError = errorOfCoordinate(formValues.latitude, 'Szerokość geograficzna', MINIMUM_LATITUDE, MAXIMUM_LATITUDE);
    if (latitudeError) errorMessageByFieldName.latitude = latitudeError;
    const longitudeError = errorOfCoordinate(formValues.longitude, 'Długość geograficzna', MINIMUM_LONGITUDE, MAXIMUM_LONGITUDE);
    if (longitudeError) errorMessageByFieldName.longitude = longitudeError;
    return errorMessageByFieldName;
  }
  const xError = errorOfGridMeters(formValues.gridX, 'Współrzędna X');
  if (xError) errorMessageByFieldName.gridX = xError;
  const yError = errorOfGridMeters(formValues.gridY, 'Współrzędna Y');
  if (yError) errorMessageByFieldName.gridY = yError;
  if (xError || yError) return errorMessageByFieldName;
  const { latitude, longitude } = toWgs84Point(formValues);
  const isInsidePoland = latitude >= POLAND_WITH_MARGIN.minimumLatitude && latitude <= POLAND_WITH_MARGIN.maximumLatitude
    && longitude >= POLAND_WITH_MARGIN.minimumLongitude && longitude <= POLAND_WITH_MARGIN.maximumLongitude;
  if (!isInsidePoland) {
    errorMessageByFieldName.gridX = 'Punkt wypada poza Polską - sprawdź strefę układu i czy X (północ) i Y (wschód) nie są zamienione.';
  }
  return errorMessageByFieldName;
};

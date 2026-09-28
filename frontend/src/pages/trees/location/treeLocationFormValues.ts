import type { TreeLocation, TreeLocationRequest } from '../treeDataTypes';

/**
 * Wartosci recznego formularza wspolrzednych - tekst az do wyslania (pozwala wpisac przecinek dziesietny
 * i pokazac czytelny blad zamiast NaN). Dotyczy tylko formularza: mapa/GNSS beda tworzyc TreeLocationRequest same.
 */
export interface TreeLocationFormValues {
  latitude: string;
  longitude: string;
}

/** Zakresy jak w backendzie (TreeLocation) - backend waliduje ponownie i pozostaje zrodlem prawdy. */
const MINIMUM_LATITUDE = -90;
const MAXIMUM_LATITUDE = 90;
const MINIMUM_LONGITUDE = -180;
const MAXIMUM_LONGITUDE = 180;

/** Liczba dziesietna ze znakiem, kropka albo przecinkiem, np. 52.2297, -0,1276. */
const SIGNED_DECIMAL_DEGREES_PATTERN = /^-?\d+([.,]\d+)?$/;

export const toTreeLocationFormValues = (location: TreeLocation | null | undefined): TreeLocationFormValues => ({
  latitude: location?.latitude.toString() ?? '',
  longitude: location?.longitude.toString() ?? '',
});

const toDegrees = (fieldValue: string) => Number(fieldValue.trim().replace(',', '.'));

export const toTreeLocationRequest = (formValues: TreeLocationFormValues): TreeLocationRequest => ({
  latitude: toDegrees(formValues.latitude),
  longitude: toDegrees(formValues.longitude),
});

/** Blad jednej wspolrzednej albo undefined, gdy jest poprawna. Obie sa wymagane - brak lokalizacji to "Remove". */
const errorOfCoordinate = (fieldValue: string, coordinateName: string, minimum: number, maximum: number) => {
  const coordinateText = fieldValue.trim();
  if (!coordinateText) return `${coordinateName} is required.`;
  if (!SIGNED_DECIMAL_DEGREES_PATTERN.test(coordinateText)) {
    return `${coordinateName} must be a number in decimal degrees, e.g. 52.2297.`;
  }
  const degrees = toDegrees(coordinateText);
  return degrees >= minimum && degrees <= maximum
    ? undefined
    : `${coordinateName} must be between ${minimum} and ${maximum}.`;
};

/** Walidacja w przegladarce - pusty wynik oznacza, ze formularz mozna wyslac. */
export const validateTreeLocationFormValues = (formValues: TreeLocationFormValues): Record<string, string> => {
  const errorMessageByFieldName: Record<string, string> = {};
  const latitudeError = errorOfCoordinate(formValues.latitude, 'Latitude', MINIMUM_LATITUDE, MAXIMUM_LATITUDE);
  if (latitudeError) errorMessageByFieldName.latitude = latitudeError;
  const longitudeError = errorOfCoordinate(formValues.longitude, 'Longitude', MINIMUM_LONGITUDE, MAXIMUM_LONGITUDE);
  if (longitudeError) errorMessageByFieldName.longitude = longitudeError;
  return errorMessageByFieldName;
};

import type { TreeLocation, TreeLocationRequest } from '../treeDataTypes';
import { MAXIMUM_LATITUDE, MAXIMUM_LONGITUDE, MINIMUM_LATITUDE, MINIMUM_LONGITUDE } from './treeLocationValidRange';

/**
 * Wartosci recznego formularza wspolrzednych - tekst az do wyslania (pozwala wpisac przecinek dziesietny
 * i pokazac czytelny blad zamiast NaN). Dotyczy tylko formularza: mapa tworzy TreeLocationRequest sama (treeLocationMapCoordinates).
 */
export interface TreeLocationFormValues {
  latitude: string;
  longitude: string;
}

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

/** Blad jednej wspolrzednej albo undefined, gdy jest poprawna. Obie sa wymagane - brak lokalizacji to "Usun". */
const errorOfCoordinate = (fieldValue: string, coordinateName: string, minimum: number, maximum: number) => {
  const coordinateText = fieldValue.trim();
  if (!coordinateText) return `${coordinateName} jest wymagana.`;
  if (!SIGNED_DECIMAL_DEGREES_PATTERN.test(coordinateText)) {
    return `${coordinateName} musi być liczbą w stopniach dziesiętnych, np. 52.2297.`;
  }
  const degrees = toDegrees(coordinateText);
  return degrees >= minimum && degrees <= maximum
    ? undefined
    : `${coordinateName} musi być w zakresie od ${minimum} do ${maximum}.`;
};

/** Walidacja w przegladarce - pusty wynik oznacza, ze formularz mozna wyslac. */
export const validateTreeLocationFormValues = (formValues: TreeLocationFormValues): Record<string, string> => {
  const errorMessageByFieldName: Record<string, string> = {};
  const latitudeError = errorOfCoordinate(formValues.latitude, 'Szerokość geograficzna', MINIMUM_LATITUDE, MAXIMUM_LATITUDE);
  if (latitudeError) errorMessageByFieldName.latitude = latitudeError;
  const longitudeError = errorOfCoordinate(formValues.longitude, 'Długość geograficzna', MINIMUM_LONGITUDE, MAXIMUM_LONGITUDE);
  if (longitudeError) errorMessageByFieldName.longitude = longitudeError;
  return errorMessageByFieldName;
};

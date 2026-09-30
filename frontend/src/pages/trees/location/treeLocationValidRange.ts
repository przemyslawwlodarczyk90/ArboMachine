import type { TreeLocation } from '../treeDataTypes';

// Zakresy wspolrzednych WGS84 - takie same jak w backendzie (TreeLocation). Backend waliduje ponownie
// i pozostaje zrodlem prawdy. Celowo bez ograniczenia "tylko Polska" - system moze dzialac takze poza nia.
// Modul bez importow wartosci - testy (node --test) laduja go bezposrednio.

export const MINIMUM_LATITUDE = -90;
export const MAXIMUM_LATITUDE = 90;
export const MINIMUM_LONGITUDE = -180;
export const MAXIMUM_LONGITUDE = 180;

/** Czy punkt ma skonczone wspolrzedne w zakresie WGS84 (chroni przed oczywistymi bledami przed wyslaniem do API). */
export const isTreeLocationWithinValidRange = ({ latitude, longitude }: TreeLocation) =>
  Number.isFinite(latitude) && Number.isFinite(longitude)
  && latitude >= MINIMUM_LATITUDE && latitude <= MAXIMUM_LATITUDE
  && longitude >= MINIMUM_LONGITUDE && longitude <= MAXIMUM_LONGITUDE;

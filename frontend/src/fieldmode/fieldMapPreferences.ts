// Etap 32: ustawienia mapy w trybie terenowym zapamietane na urzadzeniu - tryb widoku mapy (V1: domyslnie
// "Oznaczenia drzew", bo oznaczenia to glowna informacja zbierana w terenie) i pomiar usredniony zamiast pojedynczego.
// Modul bez React - testowany przez node --test z podstawionym magazynem (wzor: basemapPreference.ts).
import type { KeyValueStorage } from '../map/basemapPreference.ts';
import { MAP_VIEW_MODES, type MapViewMode } from '../pages/trees/map/mapViewMode.ts';

export const FIELD_MAP_VIEW_MODE_STORAGE_KEY = 'arbomachine.field.mapViewMode';
export const FIELD_GNSS_AVERAGING_STORAGE_KEY = 'arbomachine.field.gnssAveraging';
export const DEFAULT_FIELD_MAP_VIEW_MODE: MapViewMode = 'MARKINGS';

const readItem = (storage: KeyValueStorage | null, key: string): string | null => {
  try {
    return storage?.getItem(key) ?? null;
  } catch {
    return null;
  }
};

const writeItem = (storage: KeyValueStorage | null, key: string, value: string) => {
  try {
    storage?.setItem(key, value);
  } catch {
    // Zablokowany magazyn - ustawienie dziala do zamkniecia strony.
  }
};

export function readFieldMapViewMode(storage: KeyValueStorage | null): MapViewMode {
  const saved = readItem(storage, FIELD_MAP_VIEW_MODE_STORAGE_KEY);
  return MAP_VIEW_MODES.some(({ mode }) => mode === saved) ? saved as MapViewMode : DEFAULT_FIELD_MAP_VIEW_MODE;
}

export const saveFieldMapViewMode = (storage: KeyValueStorage | null, mode: MapViewMode) =>
  writeItem(storage, FIELD_MAP_VIEW_MODE_STORAGE_KEY, mode);

/** V1: domyslnie pomiar pojedynczy (szybko); usrednianie wlacza przelacznik "Usrednij". */
export const readFieldGnssAveraging = (storage: KeyValueStorage | null): boolean =>
  readItem(storage, FIELD_GNSS_AVERAGING_STORAGE_KEY) === 'true';

export const saveFieldGnssAveraging = (storage: KeyValueStorage | null, isAveraging: boolean) =>
  writeItem(storage, FIELD_GNSS_AVERAGING_STORAGE_KEY, String(isAveraging));

// Wybor podkladu mapy (etap 31) - wspolny dla wszystkich map aplikacji i zapamietany na urzadzeniu (localStorage).
// Modul bez OpenLayers i bez React - testowany przez node --test z podstawionym magazynem.

export type BasemapId = 'OSM' | 'GEOPORTAL_ORTHOPHOTO' | 'GEOPORTAL_TOPO' | 'GOOGLE';

export interface BasemapOption {
  id: BasemapId;
  label: string;
  /** false = zaslepka w UI (opcja widoczna, ale nie do wybrania) - powod w toBeClarifiedNote */
  isAvailable: boolean;
  toBeClarifiedNote?: string;
}

export const BASEMAP_OPTIONS: BasemapOption[] = [
  { id: 'OSM', label: 'OpenStreetMap', isAvailable: true },
  { id: 'GEOPORTAL_ORTHOPHOTO', label: 'Ortofotomapa (Geoportal)', isAvailable: true },
  { id: 'GEOPORTAL_TOPO', label: 'Mapa topograficzna (Geoportal)', isAvailable: true },
  {
    id: 'GOOGLE', label: 'Google Maps', isAvailable: false,
    toBeClarifiedNote: 'Google Maps wymaga klucza API i akceptacji licencji (płatne powyżej limitu) - czy Geoportal + OSM wystarczą?',
  },
];

export interface BasemapSelection {
  basemapId: BasemapId;
  /** nakladka dzialek ewidencyjnych (KIEG / GUGiK) nad podkladem */
  showParcels: boolean;
}

export const DEFAULT_BASEMAP_SELECTION: BasemapSelection = { basemapId: 'OSM', showParcels: false };

/** Minimalny interfejs localStorage (do podstawienia w testach). */
export interface KeyValueStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
}

export const BASEMAP_STORAGE_KEY = 'arbomachine.basemap';

const isSelectableBasemapId = (value: unknown): value is BasemapId =>
  BASEMAP_OPTIONS.some((option) => option.id === value && option.isAvailable);

/** Odczyt zapamietanego wyboru; uszkodzony / niedostepny wpis albo brak magazynu = wybor domyslny. */
export function readBasemapSelection(storage: KeyValueStorage | null): BasemapSelection {
  try {
    const parsed: unknown = JSON.parse(storage?.getItem(BASEMAP_STORAGE_KEY) ?? 'null');
    if (!parsed || typeof parsed !== 'object') return DEFAULT_BASEMAP_SELECTION;
    const { basemapId, showParcels } = parsed as Record<string, unknown>;
    return {
      basemapId: isSelectableBasemapId(basemapId) ? basemapId : DEFAULT_BASEMAP_SELECTION.basemapId,
      showParcels: showParcels === true,
    };
  } catch {
    return DEFAULT_BASEMAP_SELECTION;
  }
}

/** Prosty magazyn stanu z subskrypcja (pod useSyncExternalStore i pod mapy OpenLayers). */
export function createBasemapPreferenceStore(storage: KeyValueStorage | null) {
  let selection = readBasemapSelection(storage);
  const listeners = new Set<() => void>();
  return {
    getSelection: () => selection,
    subscribe(listener: () => void) {
      listeners.add(listener);
      return () => { listeners.delete(listener); };
    },
    update(change: Partial<BasemapSelection>) {
      const next = { ...selection, ...change };
      if (!isSelectableBasemapId(next.basemapId)) return;
      selection = next;
      try {
        storage?.setItem(BASEMAP_STORAGE_KEY, JSON.stringify(selection));
      } catch {
        // Tryb prywatny / zablokowany magazyn - wybor dziala do zamkniecia strony.
      }
      listeners.forEach((listener) => listener());
    },
  };
}

function browserStorageOrNull(): KeyValueStorage | null {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage;
  } catch {
    return null;
  }
}

/** Jeden wspolny magazyn aplikacji - zmiana podkladu na jednej mapie dotyczy wszystkich map. */
export const basemapPreferenceStore = createBasemapPreferenceStore(browserStorageOrNull());

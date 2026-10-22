// Etap 32: rejestr niezapisanych zmian (formularz karty drzewa, panel drzewa w terenie) - zeby przelaczenie
// widok biurowy <-> tryb terenowy zapytalo o decyzje zamiast po cichu porzucic wpisane dane.
// Modul bez React - testowany przez node --test; komponenty czytaja go przez useSyncExternalStore.

/** Zrodlo niezapisanych zmian; save (opcjonalnie) zapisuje je i zwraca true, gdy zapis sie udal. */
export interface UnsavedChangesSource {
  save?: () => Promise<boolean>;
  discard?: () => void;
}

export function createUnsavedChangesStore() {
  const sources = new Map<string, UnsavedChangesSource>();
  const listeners = new Set<() => void>();
  let version = 0;
  const notify = () => {
    version += 1;
    listeners.forEach((listener) => listener());
  };
  return {
    /** null = brak niezapisanych zmian w danym zrodle */
    setSource(sourceId: string, source: UnsavedChangesSource | null) {
      if (!source && !sources.has(sourceId)) return;
      if (source) sources.set(sourceId, source); else sources.delete(sourceId);
      notify();
    },
    hasUnsavedChanges: () => sources.size > 0,
    /** Czy wszystkie zrodla da sie zapisac jednym przyciskiem "Zapisz". */
    canSaveAll: () => sources.size > 0 && [...sources.values()].every((source) => source.save !== undefined),
    /** Zapisuje kolejno wszystkie zrodla; false = ktorys zapis sie nie udal (zrodlo pokazuje wtedy wlasny blad). */
    async saveAll(): Promise<boolean> {
      for (const [sourceId, source] of [...sources.entries()]) {
        if (!source.save || !(await source.save())) return false;
        sources.delete(sourceId);
      }
      notify();
      return true;
    },
    discardAll() {
      for (const source of sources.values()) source.discard?.();
      sources.clear();
      notify();
    },
    subscribe(listener: () => void) {
      listeners.add(listener);
      return () => { listeners.delete(listener); };
    },
    getVersion: () => version,
  };
}

export type UnsavedChangesStore = ReturnType<typeof createUnsavedChangesStore>;

/** Jeden rejestr na aplikacje. */
export const unsavedChangesStore = createUnsavedChangesStore();

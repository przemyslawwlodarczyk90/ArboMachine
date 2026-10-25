import { useEffect, useRef } from 'react';
import { unsavedChangesStore } from './unsavedChangesStore';

/**
 * Etap 48: zgloszenie niezapisanych zmian formularza do rejestru aplikacji (pytanie "Zapisz / Odrzuc / Zostan"
 * przy linkach, "Wstecz", przelaczaniu trybu i wyborze innego drzewa). save i discard sa czytane z refa - rejestr
 * zawsze wywoluje najnowsza wersje, bez ponownego zglaszania przy kazdym renderze.
 * Etap 50: save = null - okno nie proponuje "Zapisz" (np. okna tworzenia: zapis oznacza nowy rekord i przejscie
 * do niego, co kloci sie z wybrana nawigacja); zostaje "Odrzuc zmiany / Zostan".
 */
export function useUnsavedChangesSource(sourceId: string, hasUnsavedChanges: boolean, label: string,
                                        save: (() => Promise<boolean>) | null, discard: () => void) {
  const saveRef = useRef(save);
  const discardRef = useRef(discard);
  saveRef.current = save;
  discardRef.current = discard;
  const canSave = save !== null;
  useEffect(() => {
    unsavedChangesStore.setSource(sourceId, hasUnsavedChanges
      ? { label, save: canSave ? () => saveRef.current?.() ?? Promise.resolve(false) : undefined, discard: () => discardRef.current() }
      : null);
    return () => unsavedChangesStore.setSource(sourceId, null);
  }, [sourceId, hasUnsavedChanges, label, canSave]);
}

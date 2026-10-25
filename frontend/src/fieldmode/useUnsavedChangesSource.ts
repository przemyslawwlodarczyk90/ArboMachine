import { useEffect, useRef } from 'react';
import { unsavedChangesStore } from './unsavedChangesStore';

/**
 * Etap 48: zgloszenie niezapisanych zmian formularza do rejestru aplikacji (pytanie "Zapisz / Odrzuc / Zostan"
 * przy linkach, "Wstecz", przelaczaniu trybu i wyborze innego drzewa). save i discard sa czytane z refa - rejestr
 * zawsze wywoluje najnowsza wersje, bez ponownego zglaszania przy kazdym renderze.
 */
export function useUnsavedChangesSource(sourceId: string, hasUnsavedChanges: boolean, label: string,
                                        save: () => Promise<boolean>, discard: () => void) {
  const saveRef = useRef(save);
  const discardRef = useRef(discard);
  saveRef.current = save;
  discardRef.current = discard;
  useEffect(() => {
    unsavedChangesStore.setSource(sourceId, hasUnsavedChanges
      ? { label, save: () => saveRef.current(), discard: () => discardRef.current() }
      : null);
    return () => unsavedChangesStore.setSource(sourceId, null);
  }, [sourceId, hasUnsavedChanges, label]);
}

import { useEffect } from 'react';
import { useBlocker } from 'react-router-dom';
import { UnsavedChangesDialog } from './UnsavedChangesDialog';
import { isLeavingCurrentScreen } from './unsavedChangesLeaveTarget';
import { unsavedChangesStore } from './unsavedChangesStore';

/**
 * Ochrona niezapisanych zmian (formularz karty drzewa, panel drzewa w terenie) przy opuszczaniu ekranu:
 * - kazde przejscie na inna strone aplikacji - link, przycisk "Wstecz"/"Dalej" przegladarki, gest wstecz, nawigacja
 *   z kodu (etap 47, useBlocker routera z danymi) -> okno "Zapisz / Odrzuć zmiany / Zostań" z etapu 32,
 * - zamkniecie albo odswiezenie karty przegladarki -> systemowe pytanie przegladarki (etap 46).
 * Akcje pilnowane juz wczesniej (runGuarded: przelaczenie trybu, inne drzewo w terenie) czyszcza rejestr przed
 * nawigacja, wiec nie pytaja drugi raz. Montowany raz, w korzeniu tras.
 */
export function UnsavedChangesLeaveGuard() {
  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    unsavedChangesStore.hasUnsavedChanges() && isLeavingCurrentScreen(currentLocation, nextLocation));

  useEffect(() => {
    const warnBeforeUnload = (unloadEvent: BeforeUnloadEvent) => {
      if (!unsavedChangesStore.hasUnsavedChanges()) return;
      unloadEvent.preventDefault();
      unloadEvent.returnValue = '';
    };
    window.addEventListener('beforeunload', warnBeforeUnload);
    return () => window.removeEventListener('beforeunload', warnBeforeUnload);
  }, []);

  if (blocker.state !== 'blocked') return null;
  return <UnsavedChangesDialog proceed={() => blocker.proceed()} closeDialog={() => blocker.reset()} />;
}

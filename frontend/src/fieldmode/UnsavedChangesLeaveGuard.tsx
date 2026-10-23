import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { UnsavedChangesDialog } from './UnsavedChangesDialog';
import { internalLeaveTarget } from './unsavedChangesLeaveTarget';
import { unsavedChangesStore } from './unsavedChangesStore';

/**
 * Etap 46: ochrona niezapisanych zmian (formularz karty drzewa, panel drzewa w terenie) przy opuszczaniu ekranu -
 * dotad pytaly tylko przelaczanie trybu i wybor innego drzewa w terenie:
 * - klikniecie w link aplikacji (menu, "Wróć do listy drzew", zakladki projektu) -> okno "Zapisz / Odrzuć / Zostań",
 * - zamkniecie albo odswiezenie karty przegladarki -> systemowe pytanie przegladarki.
 * Przycisk "Wstecz" przegladarki NIE jest pilnowany (BrowserRouter bez blokady nawigacji) - zob. DO_DOPRECYZOWANIA.
 * Montowany raz, wewnatrz routera.
 */
export function UnsavedChangesLeaveGuard() {
  const navigate = useNavigate();
  const [pendingTarget, setPendingTarget] = useState<string | null>(null);

  useEffect(() => {
    const warnBeforeUnload = (unloadEvent: BeforeUnloadEvent) => {
      if (!unsavedChangesStore.hasUnsavedChanges()) return;
      unloadEvent.preventDefault();
      unloadEvent.returnValue = '';
    };
    // Faza przechwytywania: przed obsluga <Link> z React Routera, ktora nasluchuje na korzeniu aplikacji.
    const interceptLinkClick = (clickEvent: MouseEvent) => {
      if (clickEvent.defaultPrevented || !unsavedChangesStore.hasUnsavedChanges()) return;
      const anchor = clickEvent.target instanceof Element ? clickEvent.target.closest('a') : null;
      if (!anchor) return;
      const target = internalLeaveTarget({
        href: anchor.getAttribute('href'),
        target: anchor.getAttribute('target'),
        hasDownloadAttribute: anchor.hasAttribute('download'),
        opensElsewhere: clickEvent.button !== 0 || clickEvent.metaKey || clickEvent.ctrlKey || clickEvent.shiftKey || clickEvent.altKey,
      }, window.location.href);
      if (!target) return;
      clickEvent.preventDefault();
      clickEvent.stopPropagation();
      setPendingTarget(target);
    };
    window.addEventListener('beforeunload', warnBeforeUnload);
    document.addEventListener('click', interceptLinkClick, true);
    return () => {
      window.removeEventListener('beforeunload', warnBeforeUnload);
      document.removeEventListener('click', interceptLinkClick, true);
    };
  }, []);

  if (!pendingTarget) return null;
  return (
    <UnsavedChangesDialog
      proceed={() => { setPendingTarget(null); navigate(pendingTarget); }}
      closeDialog={() => setPendingTarget(null)} />
  );
}

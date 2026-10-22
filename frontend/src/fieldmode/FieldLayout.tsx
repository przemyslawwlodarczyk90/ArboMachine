import { useMemo, useState, type ReactNode } from 'react';
import { Link, Outlet, useOutletContext } from 'react-router-dom';
import { OfflineStatusBar } from '../offline/OfflineStatusBar';
import { useOfflineState } from '../offline/useOfflineState';
import { WorkModeSwitchButton } from './WorkModeSwitchButton';

/** Co ekran trybu terenowego wstawia do gornego paska (nazwa projektu, wskaznik GPS). */
export interface FieldTopBarContent {
  title: string | null;
  /** powrot do listy projektow (brak = jestesmy na liscie) */
  showsBackLink: boolean;
  gpsIndicator: ReactNode;
}

const EMPTY_TOP_BAR: FieldTopBarContent = { title: null, showsBackLink: false, gpsIndicator: null };

interface FieldLayoutContext {
  setTopBarContent: (content: FieldTopBarContent) => void;
}

/** Ekrany trybu terenowego ustawiaja tresc gornego paska przez ten hook. */
export const useFieldTopBar = () => useOutletContext<FieldLayoutContext>().setTopBarContent;

const changesLabel = (count: number) => (count === 1 ? '1 zmiana' : count >= 2 && count <= 4 ? `${count} zmiany` : `${count} zmian`);

/**
 * Uklad trybu terenowego (etap 32): pelna wysokosc ekranu bez przewijania strony i bez kolumny .content; waski gorny
 * pasek (wstecz, nazwa projektu, GPS, polaczenie i kolejka zmian, "Widok biurowy"), pod nim zwarty pasek offline
 * i ekran (mapa albo lista). Ten sam straznik roli i te same API co widok biurowy.
 */
export function FieldLayout() {
  const [topBarContent, setTopBarContent] = useState<FieldTopBarContent>(EMPTY_TOP_BAR);
  const { isOnline, pendingChangeCount, pendingPhotoCount } = useOfflineState();
  const context = useMemo<FieldLayoutContext>(() => ({ setTopBarContent }), []);

  return (
    <div className="field-shell">
      <header className="field-topbar">
        {topBarContent.showsBackLink ? (
          <Link to="/field" className="btn btn-secondary field-topbar-back" aria-label="Wróć do listy projektów">←</Link>
        ) : (
          <span className="field-topbar-brand">ArboMachine</span>
        )}
        <span className="field-topbar-title" title={topBarContent.title ?? undefined}>{topBarContent.title ?? 'Tryb terenowy'}</span>
        {topBarContent.gpsIndicator}
        <span className={`field-connection ${isOnline ? 'field-connection-online' : 'field-connection-offline'}`}
              role="status" title={isOnline ? 'Połączono z serwerem' : 'Brak połączenia - zmiany zapisują się na urządzeniu'}>
          <span className="field-connection-dot" aria-hidden="true" />
          <span className="field-connection-text">{isOnline ? 'online' : 'offline'}</span>
          {pendingChangeCount > 0 && <span className="field-connection-pending"> · {changesLabel(pendingChangeCount)}</span>}
          {pendingPhotoCount > 0 && <span className="field-connection-pending"> · zdjęcia: {pendingPhotoCount}</span>}
        </span>
        <WorkModeSwitchButton targetMode="OFFICE" className="btn btn-secondary field-topbar-office" />
      </header>
      <OfflineStatusBar isCompact />
      <main className="field-main">
        <Outlet context={context} />
      </main>
    </div>
  );
}

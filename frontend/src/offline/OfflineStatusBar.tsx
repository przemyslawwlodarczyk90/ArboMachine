import { useEffect, useState } from 'react';
import { describePendingChange } from './offlineQueue';
import { offlineService } from './offlineService';
import { useOfflineState } from './useOfflineState';

const changesLabel = (count: number) => (count === 1 ? '1 zmiana' : count >= 2 && count <= 4 ? `${count} zmiany` : `${count} zmian`);

/**
 * Pasek stanu pracy offline (etap 26) w layoucie panelu: brak polaczenia, zmiany czekajace na wyslanie,
 * synchronizacja, blad synchronizacji (Sprobuj ponownie / Pomin te zmiane) i krotkie potwierdzenie sukcesu.
 * Niewidoczny, gdy jest polaczenie i nic nie czeka.
 */
export function OfflineStatusBar({ isCompact = false }: {
  /** etap 32 (tryb terenowy): brak polaczenia jako jedna linia, pelny opis po dotknieciu - ekran nad mapa jest cenny */
  isCompact?: boolean;
} = {}) {
  const offlineState = useOfflineState();
  const [shownSuccessGeneration, setShownSuccessGeneration] = useState(offlineState.syncGeneration);
  const [isSuccessVisible, setIsSuccessVisible] = useState(false);

  useEffect(() => {
    if (offlineState.syncGeneration === shownSuccessGeneration) return;
    setShownSuccessGeneration(offlineState.syncGeneration);
    setIsSuccessVisible(true);
    const timeout = window.setTimeout(() => setIsSuccessVisible(false), 6000);
    return () => window.clearTimeout(timeout);
  }, [offlineState.syncGeneration, shownSuccessGeneration]);

  const { isOnline, pendingChangeCount, syncStatus, failedChange } = offlineState;

  if (syncStatus === 'failed' && failedChange) {
    return (
      <div className="offline-status-bar offline-status-error" role="alert">
        <span>
          Nie udało się zsynchronizować zmiany: <strong>{describePendingChange(failedChange)}</strong>
          {failedChange.failure?.message ? ` - ${failedChange.failure.message.replace(/\.+$/, '')}` : ''}.
          {' '}Na urządzeniu czeka: {changesLabel(pendingChangeCount)} (razem z tą).
        </span>
        <span className="offline-status-actions">
          <button type="button" className="btn btn-small btn-secondary" onClick={() => void offlineService.retryFailedChange()}>
            Spróbuj ponownie
          </button>
          <button type="button" className="btn btn-small btn-danger-outline" onClick={() => void offlineService.discardFailedChange()}>
            Pomiń tę zmianę
          </button>
        </span>
      </div>
    );
  }

  if (!isOnline && isCompact) {
    return (
      <details className="offline-status-bar offline-status-offline offline-status-compact" role="status">
        <summary>
          <strong>Offline</strong> - zmiany zapisują się na urządzeniu
          {pendingChangeCount > 0 ? ` (czeka: ${changesLabel(pendingChangeCount)})` : ''}
        </summary>
        <span>
          Zmiany drzew, lokalizacji, oznaczeń i prac zostaną wysłane po połączeniu. Zdjęcia, symbole i dokumenty wymagają
          połączenia. Podkład mapy bez zasięgu się nie wczytuje.
        </span>
      </details>
    );
  }

  if (!isOnline) {
    return (
      <div className="offline-status-bar offline-status-offline" role="status">
        <span>
          <strong>Brak połączenia - pracujesz offline.</strong> Zmiany drzew, lokalizacji i prac zapisują się na tym urządzeniu
          {pendingChangeCount > 0 ? ` (czeka: ${changesLabel(pendingChangeCount)})` : ''} i zostaną wysłane po połączeniu.
          Zdjęcia, symbole i dokumenty wymagają połączenia.
        </span>
      </div>
    );
  }

  if (syncStatus === 'syncing') {
    return <div className="offline-status-bar offline-status-pending" role="status">Synchronizowanie zmian…</div>;
  }

  if (pendingChangeCount > 0) {
    return (
      <div className="offline-status-bar offline-status-pending" role="status">
        <span>Na tym urządzeniu czeka na wysłanie: {changesLabel(pendingChangeCount)}.</span>
        <button type="button" className="btn btn-small btn-primary" onClick={() => void offlineService.syncNow()}>
          Synchronizuj teraz
        </button>
      </div>
    );
  }

  if (isSuccessVisible && offlineState.lastSyncedCount > 0) {
    return (
      <div className="offline-status-bar offline-status-success" role="status">
        Zsynchronizowano: {changesLabel(offlineState.lastSyncedCount)}.
      </div>
    );
  }
  return null;
}

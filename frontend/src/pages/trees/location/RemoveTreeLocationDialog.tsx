import { useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { ModalDialog } from '../../../components/ModalDialog';
import type { Tree } from '../treeDataTypes';
import { treesApi } from '../treesApi';

/**
 * Potwierdzenie usuniecia lokalizacji (istniejace DELETE .../trees/{treeId}/location). Po sukcesie oddaje
 * drzewo z odpowiedzi (onTreeLocationRemoved); przy bledzie okno zostaje otwarte z komunikatem.
 */
export function RemoveTreeLocationDialog({ projectId, treeId, onTreeLocationRemoved, closeDialog }: {
  projectId: number;
  treeId: number;
  onTreeLocationRemoved: (changedTree: Tree) => void;
  closeDialog: () => void;
}) {
  const [isRemovingLocation, setIsRemovingLocation] = useState(false);
  const [removalErrorMessage, setRemovalErrorMessage] = useState<string | null>(null);
  // Synchroniczna blokada podwojnego klikniecia "Usun".
  const isRemovalInProgress = useRef(false);

  const removeLocation = async () => {
    if (isRemovalInProgress.current) return;
    isRemovalInProgress.current = true;
    setIsRemovingLocation(true);
    setRemovalErrorMessage(null);
    try {
      onTreeLocationRemoved(await treesApi.clearTreeLocation(projectId, treeId));
    } catch (removalError) {
      setRemovalErrorMessage(removalError instanceof ApiRequestError ? removalError.message : 'Nieoczekiwany błąd.');
      isRemovalInProgress.current = false;
      setIsRemovingLocation(false);
    }
  };

  return (
    <ModalDialog title="Usunąć lokalizację?" closeDialog={closeDialog} isClosingBlocked={isRemovingLocation}>
      <div className="form">
        {removalErrorMessage && <Alert variant="error">{removalErrorMessage}</Alert>}
        <p className="muted">Drzewo zostanie oznaczone jako bez lokalizacji. Możesz ją ustawić ponownie w każdej chwili.</p>
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isRemovingLocation}>
            Anuluj
          </button>
          <button type="button" className="btn btn-danger" onClick={removeLocation} disabled={isRemovingLocation}>
            {isRemovingLocation ? 'Usuwanie…' : 'Usuń lokalizację'}
          </button>
        </div>
      </div>
    </ModalDialog>
  );
}

import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { ToBeClarifiedNote } from '../../../components/ToBeClarifiedNote';
import { useOfflineState } from '../../../offline/useOfflineState';
import { treesApi } from '../treesApi';
import { treeDeletionBlockReason } from './treeDeletionBlockReason';

/**
 * "Usuń drzewo" na dole karty drzewa (etap 44, 02 poz. 2.10 - omylkowo dodane drzewo). Usuwa drzewo razem ze
 * zdjeciami, pracami, waloryzacja, kontrolami i oznaczeniami - bez kosza i bez cofania, dlatego dwa kroki
 * i tylko z polaczeniem (bez kolejki offline). Po usunieciu - powrot do listy drzew.
 */
export function TreeDeletionSection({ projectId, treeId, inventoryNumber, isCreatedOffline, hasPendingChanges }: {
  projectId: number;
  treeId: number;
  inventoryNumber: string;
  isCreatedOffline: boolean;
  hasPendingChanges: boolean;
}) {
  const navigate = useNavigate();
  const { isOnline } = useOfflineState();
  const [isConfirmationVisible, setIsConfirmationVisible] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const isDeletionInProgress = useRef(false);
  const blockReason = treeDeletionBlockReason({ isOnline, isCreatedOffline, hasPendingChanges });

  const deleteTree = async () => {
    if (isDeletionInProgress.current) return;
    isDeletionInProgress.current = true;
    setIsDeleting(true);
    setErrorMessage(null);
    try {
      await treesApi.deleteTreeOfProject(projectId, treeId);
      navigate(`/projects/${projectId}/trees`, { replace: true });
    } catch (deletionError) {
      setErrorMessage(deletionError instanceof ApiRequestError ? deletionError.message : 'Nieoczekiwany błąd.');
      isDeletionInProgress.current = false;
      setIsDeleting(false);
    }
  };

  return (
    <section className="card card-danger tree-deletion-section" aria-labelledby="tree-deletion-heading">
      <h3 id="tree-deletion-heading">Usuń drzewo</h3>
      <p className="muted small">
        Dla drzewa dodanego omyłkowo. Razem z drzewem zostaną trwale usunięte jego zdjęcia, zalecenia i prace,
        waloryzacja, kontrole stanu i oznaczenia; drzewo zniknie też z grup drzew. Tej operacji nie można cofnąć.
      </p>
      <ToBeClarifiedNote>
        Drzewo wycięte albo nieistniejące - czy usuwać je z projektu, czy zostawić z oznaczeniem (np. „wycięte”) dla historii?
      </ToBeClarifiedNote>
      {errorMessage && <Alert variant="error">Nie udało się usunąć drzewa. {errorMessage}</Alert>}
      {blockReason && <p className="muted small" role="status">{blockReason}</p>}
      {isConfirmationVisible && !blockReason ? (
        <div className="photo-annotation-confirmation" role="alert">
          <span>Czy na pewno usunąć drzewo <strong>{inventoryNumber}</strong> i wszystkie jego dane?</span>
          <div className="form-actions">
            <button type="button" className="btn btn-ghost" onClick={() => setIsConfirmationVisible(false)} disabled={isDeleting}>
              Anuluj
            </button>
            <button type="button" className="btn btn-danger" onClick={deleteTree} disabled={isDeleting}>
              {isDeleting ? 'Usuwanie…' : `Tak, usuń drzewo ${inventoryNumber}`}
            </button>
          </div>
        </div>
      ) : (
        <button type="button" className="btn btn-danger-outline" onClick={() => setIsConfirmationVisible(true)}
                disabled={blockReason !== null}>
          Usuń drzewo
        </button>
      )}
    </section>
  );
}

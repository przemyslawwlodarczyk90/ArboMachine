import { useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { ModalDialog } from '../../../components/ModalDialog';
import type { TreeWork } from './treeWorkDataTypes';
import { treeWorksApi } from './treeWorksApi';

/** Potwierdzenie usuniecia pracy. Po sukcesie oddaje id usunietej pracy; przy bledzie okno zostaje otwarte. */
export function DeleteTreeWorkDialog({ projectId, treeId, treeWork, onTreeWorkDeleted, closeDialog }: {
  projectId: number;
  treeId: number;
  treeWork: TreeWork;
  onTreeWorkDeleted: (deletedTreeWorkId: number) => void;
  closeDialog: () => void;
}) {
  const [isDeleting, setIsDeleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const isDeletionInProgress = useRef(false);

  const deleteWork = async () => {
    if (isDeletionInProgress.current) return;
    isDeletionInProgress.current = true;
    setIsDeleting(true);
    setErrorMessage(null);
    try {
      await treeWorksApi.deleteTreeWork(projectId, treeId, treeWork.id);
      onTreeWorkDeleted(treeWork.id);
    } catch (deletionError) {
      setErrorMessage(deletionError instanceof ApiRequestError ? deletionError.message : 'Nieoczekiwany błąd.');
      isDeletionInProgress.current = false;
      setIsDeleting(false);
    }
  };

  return (
    <ModalDialog title="Usunąć pracę?" closeDialog={closeDialog} isClosingBlocked={isDeleting}>
      <div className="form">
        {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
        <p>Czy na pewno chcesz usunąć tę pracę?</p>
        <p className="muted small">{treeWork.name}</p>
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isDeleting}>Anuluj</button>
          <button type="button" className="btn btn-danger" onClick={deleteWork} disabled={isDeleting}>
            {isDeleting ? 'Usuwanie…' : 'Usuń pracę'}
          </button>
        </div>
      </div>
    </ModalDialog>
  );
}

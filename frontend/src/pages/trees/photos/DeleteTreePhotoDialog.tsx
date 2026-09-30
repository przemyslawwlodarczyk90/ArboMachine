import { useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { ModalDialog } from '../../../components/ModalDialog';
import type { TreePhoto } from './treePhotoDataTypes';
import { treePhotosApi } from './treePhotosApi';

/** Potwierdzenie usuniecia zdjecia. Po sukcesie oddaje id usunietego zdjecia; przy bledzie okno zostaje otwarte. */
export function DeleteTreePhotoDialog({ projectId, treeId, treePhoto, onTreePhotoDeleted, closeDialog }: {
  projectId: number;
  treeId: number;
  treePhoto: TreePhoto;
  onTreePhotoDeleted: (deletedTreePhotoId: number) => void;
  closeDialog: () => void;
}) {
  const [isDeleting, setIsDeleting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const isDeletionInProgress = useRef(false);

  const deletePhoto = async () => {
    if (isDeletionInProgress.current) return;
    isDeletionInProgress.current = true;
    setIsDeleting(true);
    setErrorMessage(null);
    try {
      await treePhotosApi.deleteTreePhoto(projectId, treeId, treePhoto.id);
      onTreePhotoDeleted(treePhoto.id);
    } catch (deletionError) {
      setErrorMessage(deletionError instanceof ApiRequestError ? deletionError.message : 'Nieoczekiwany błąd.');
      isDeletionInProgress.current = false;
      setIsDeleting(false);
    }
  };

  return (
    <ModalDialog title="Usunąć zdjęcie?" closeDialog={closeDialog} isClosingBlocked={isDeleting}>
      <div className="form">
        {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
        <p>Czy na pewno chcesz usunąć to zdjęcie?</p>
        <p className="muted small">{treePhoto.originalFileName}</p>
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isDeleting}>Anuluj</button>
          <button type="button" className="btn btn-danger" onClick={deletePhoto} disabled={isDeleting}>
            {isDeleting ? 'Usuwanie…' : 'Usuń zdjęcie'}
          </button>
        </div>
      </div>
    </ModalDialog>
  );
}

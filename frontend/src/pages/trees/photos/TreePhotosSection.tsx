import { useEffect, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { ModalDialog } from '../../../components/ModalDialog';
import { formatTimestampForDisplay } from '../../../formatting/formatTimestampForDisplay';
import { AddTreePhotoDialog } from './AddTreePhotoDialog';
import { TreePhotoAnnotationDialog } from './annotations/TreePhotoAnnotationDialog';
import { DeleteTreePhotoDialog } from './DeleteTreePhotoDialog';
import type { TreePhoto } from './treePhotoDataTypes';
import { formatFileSizeForDisplay } from './treePhotoFileRules';
import { treePhotosApi } from './treePhotosApi';
import { TreePhotoTile } from './TreePhotoTile';

type TreePhotoListState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; treePhotos: TreePhoto[] };

/**
 * Sekcja "Zdjecia" karty drzewa: responsywna siatka zdjec, dodawanie, podglad i usuwanie. Ma wlasny stan listy -
 * dodanie/usuniecie zdjecia nie przeladowuje karty drzewa. Bez biblioteki galerii: podglad to zwykly ModalDialog.
 */
export function TreePhotosSection({ projectId, treeId }: { projectId: number; treeId: number }) {
  const [treePhotoListState, setTreePhotoListState] = useState<TreePhotoListState>({ status: 'loading' });
  const [isAddPhotoDialogOpen, setIsAddPhotoDialogOpen] = useState(false);
  const [previewedPhoto, setPreviewedPhoto] = useState<{ treePhoto: TreePhoto; objectUrl: string } | null>(null);
  const [photoAwaitingDeletion, setPhotoAwaitingDeletion] = useState<TreePhoto | null>(null);
  const [annotatedPhoto, setAnnotatedPhoto] = useState<TreePhoto | null>(null);

  useEffect(() => {
    let isResponseStillWanted = true;
    setTreePhotoListState({ status: 'loading' });
    treePhotosApi.listTreePhotos(projectId, treeId)
      .then((treePhotos) => {
        if (isResponseStillWanted) setTreePhotoListState({ status: 'loaded', treePhotos });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setTreePhotoListState({
          status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.',
        });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId, treeId]);

  const appendAddedPhotoAndCloseDialog = (addedTreePhoto: TreePhoto) => {
    setTreePhotoListState((currentState) => ({
      status: 'loaded',
      treePhotos: currentState.status === 'loaded' ? [...currentState.treePhotos, addedTreePhoto] : [addedTreePhoto],
    }));
    setIsAddPhotoDialogOpen(false);
  };

  // Po zapisie/usunieciu adnotacji aktualizujemy tylko to zdjecie (bez przeladowania listy); zmiana daty
  // adnotacji odswieza miniature w wersji do dokumentacji.
  const updateAnnotationStateOfPhoto = (treePhotoId: number,
                                        annotationState: { hasAnnotation: boolean; annotationUpdatedAt: string | null }) =>
    setTreePhotoListState((currentState) => currentState.status === 'loaded'
      ? { status: 'loaded', treePhotos: currentState.treePhotos.map((treePhoto) =>
        treePhoto.id === treePhotoId ? { ...treePhoto, ...annotationState } : treePhoto) }
      : currentState);

  // Zdjecie znika z listy dopiero po potwierdzeniu backendu - kafelek odmontowuje sie i zwalnia swoj adres blob.
  const removeDeletedPhoto = (deletedTreePhotoId: number) => {
    setTreePhotoListState((currentState) => currentState.status === 'loaded'
      ? { status: 'loaded', treePhotos: currentState.treePhotos.filter((treePhoto) => treePhoto.id !== deletedTreePhotoId) }
      : currentState);
    setPhotoAwaitingDeletion(null);
    setPreviewedPhoto(null);
  };

  return (
    <section className="tree-card-section" aria-labelledby="tree-photos-heading">
      <div className="section-header-row">
        <h3 id="tree-photos-heading">Zdjęcia</h3>
        <button className="btn btn-secondary" onClick={() => setIsAddPhotoDialogOpen(true)}>Dodaj zdjęcie</button>
      </div>

      {treePhotoListState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}

      {treePhotoListState.status === 'failed' && (
        <Alert variant="error">Nie udało się wczytać zdjęć drzewa. {treePhotoListState.errorMessage}</Alert>
      )}

      {treePhotoListState.status === 'loaded' && treePhotoListState.treePhotos.length === 0 && (
        <p className="muted">To drzewo nie ma jeszcze zdjęć.</p>
      )}

      {treePhotoListState.status === 'loaded' && treePhotoListState.treePhotos.length > 0 && (
        <ul className="tree-photo-grid">
          {treePhotoListState.treePhotos.map((treePhoto) => (
            <TreePhotoTile key={treePhoto.id} projectId={projectId} treeId={treeId} treePhoto={treePhoto}
                           openPreview={(photo, objectUrl) => setPreviewedPhoto({ treePhoto: photo, objectUrl })}
                           openAnnotationEditor={setAnnotatedPhoto}
                           requestRemoval={setPhotoAwaitingDeletion} />
          ))}
        </ul>
      )}

      {isAddPhotoDialogOpen && (
        <AddTreePhotoDialog projectId={projectId} treeId={treeId} onTreePhotoAdded={appendAddedPhotoAndCloseDialog}
                            closeDialog={() => setIsAddPhotoDialogOpen(false)} />
      )}

      {previewedPhoto && (
        <ModalDialog title={previewedPhoto.treePhoto.originalFileName} closeDialog={() => setPreviewedPhoto(null)}>
          <div className="stack">
            <img className="tree-photo-preview-image" src={previewedPhoto.objectUrl}
                 alt={previewedPhoto.treePhoto.originalFileName} />
            <span className="muted small">
              Dodano {formatTimestampForDisplay(previewedPhoto.treePhoto.createdAt)}
              {' · '}{formatFileSizeForDisplay(previewedPhoto.treePhoto.fileSizeBytes)}
            </span>
            <div className="form-actions">
              <button type="button" className="btn btn-danger-outline"
                      onClick={() => setPhotoAwaitingDeletion(previewedPhoto.treePhoto)}>Usuń</button>
              {/* Pobranie z juz wczytanego adresu blob: - bez ponownego zapytania i bez tokenu w URL. */}
              <a className="btn btn-secondary" href={previewedPhoto.objectUrl}
                 download={previewedPhoto.treePhoto.hasAnnotation
                   ? `${previewedPhoto.treePhoto.originalFileName.replace(/\.[^.]+$/, '')}-adnotacja.png`
                   : previewedPhoto.treePhoto.originalFileName}>Pobierz</a>
              <button type="button" className="btn btn-primary" onClick={() => setPreviewedPhoto(null)}>Zamknij</button>
            </div>
          </div>
        </ModalDialog>
      )}

      {annotatedPhoto && (
        <TreePhotoAnnotationDialog projectId={projectId} treeId={treeId} treePhoto={annotatedPhoto}
                                   onAnnotationChanged={(annotationState) =>
                                     updateAnnotationStateOfPhoto(annotatedPhoto.id, annotationState)}
                                   closeDialog={() => setAnnotatedPhoto(null)} />
      )}

      {photoAwaitingDeletion && (
        <DeleteTreePhotoDialog projectId={projectId} treeId={treeId} treePhoto={photoAwaitingDeletion}
                               onTreePhotoDeleted={removeDeletedPhoto}
                               closeDialog={() => setPhotoAwaitingDeletion(null)} />
      )}
    </section>
  );
}

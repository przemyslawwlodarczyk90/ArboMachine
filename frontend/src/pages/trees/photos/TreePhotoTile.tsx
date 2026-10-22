import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { useOfflineState } from '../../../offline/useOfflineState';
import type { TreePhoto } from './treePhotoDataTypes';
import { useTreePhotoObjectUrl } from './useTreePhotoObjectUrl';

/**
 * Kafelek galerii: miniatura w wersji do dokumentacji (z adnotacja, jesli istnieje - pelne zdjecie przeskalowane
 * w CSS, backend nie generuje miniatur w V1), plakietka "Adnotowane", nazwa pliku, "Adnotuj"/"Edytuj adnotacje"
 * i "Usun". Klikniecie miniatury otwiera wiekszy podglad. Blad pobrania tresci pokazuje komunikat zamiast
 * "zepsutego" obrazka.
 */
export function TreePhotoTile({ projectId, treeId, treePhoto, openPreview, openAnnotationEditor, requestRemoval }: {
  projectId: number;
  treeId: number;
  treePhoto: TreePhoto;
  openPreview: (treePhoto: TreePhoto, objectUrl: string) => void;
  openAnnotationEditor: (treePhoto: TreePhoto) => void;
  requestRemoval: (treePhoto: TreePhoto) => void;
}) {
  // Wersja (data adnotacji) w zaleznosciach: po zapisie adnotacji miniatura pobiera sie ponownie.
  const objectUrlState = useTreePhotoObjectUrl(projectId, treeId, treePhoto.id, 'documentation',
    treePhoto.annotationUpdatedAt ?? null);
  const { isOnline } = useOfflineState();

  return (
    <li className="tree-photo-tile">
      {objectUrlState.status === 'loaded' ? (
        <button type="button" className="tree-photo-thumbnail"
                onClick={() => openPreview(treePhoto, objectUrlState.objectUrl)}
                aria-label={`Powiększ zdjęcie ${treePhoto.originalFileName}`}>
          <img src={objectUrlState.objectUrl} alt={treePhoto.originalFileName} loading="lazy" />
        </button>
      ) : (
        <div className="tree-photo-thumbnail tree-photo-thumbnail-placeholder">
          {objectUrlState.status === 'loading'
            ? <LoadingSpinner />
            : <span className="muted small">{isOnline ? 'Nie udało się wczytać zdjęcia.' : 'Podgląd niedostępny bez zasięgu.'}</span>}
        </div>
      )}
      {treePhoto.hasAnnotation && <span className="badge tree-photo-annotated-badge">Adnotowane</span>}
      <div className="tree-photo-tile-footer">
        <span className="tree-photo-tile-name small" title={treePhoto.originalFileName}>{treePhoto.originalFileName}</span>
        <div className="tree-photo-tile-actions">
          <button type="button" className="btn btn-secondary btn-small" onClick={() => openAnnotationEditor(treePhoto)}>
            {treePhoto.hasAnnotation ? 'Edytuj adnotacje' : 'Adnotuj'}
          </button>
          <button type="button" className="btn btn-danger-outline btn-small" onClick={() => requestRemoval(treePhoto)}>
            Usuń
          </button>
        </div>
      </div>
    </li>
  );
}

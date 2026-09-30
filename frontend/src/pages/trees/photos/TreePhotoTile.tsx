import { LoadingSpinner } from '../../../components/LoadingSpinner';
import type { TreePhoto } from './treePhotoDataTypes';
import { useTreePhotoObjectUrl } from './useTreePhotoObjectUrl';

/**
 * Kafelek galerii: miniatura (pelne zdjecie przeskalowane w CSS - backend nie generuje miniatur w V1),
 * nazwa pliku i "Usun". Klikniecie miniatury otwiera wiekszy podglad. Blad pobrania tresci pokazuje
 * komunikat zamiast "zepsutego" obrazka.
 */
export function TreePhotoTile({ projectId, treeId, treePhoto, openPreview, requestRemoval }: {
  projectId: number;
  treeId: number;
  treePhoto: TreePhoto;
  openPreview: (treePhoto: TreePhoto, objectUrl: string) => void;
  requestRemoval: (treePhoto: TreePhoto) => void;
}) {
  const objectUrlState = useTreePhotoObjectUrl(projectId, treeId, treePhoto.id);

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
            : <span className="muted small">Nie udało się wczytać zdjęcia.</span>}
        </div>
      )}
      <div className="tree-photo-tile-footer">
        <span className="tree-photo-tile-name small" title={treePhoto.originalFileName}>{treePhoto.originalFileName}</span>
        <button type="button" className="btn btn-danger-outline btn-small" onClick={() => requestRemoval(treePhoto)}>
          Usuń
        </button>
      </div>
    </li>
  );
}

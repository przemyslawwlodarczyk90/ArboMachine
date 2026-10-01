import { LoadingSpinner } from '../../../components/LoadingSpinner';
import type { ProjectPhoto } from '../../trees/photos/treePhotoDataTypes';
import { useTreePhotoObjectUrl } from '../../trees/photos/useTreePhotoObjectUrl';

/**
 * Zdjecie w materialach do dokumentacji: podglad z .../documentation-content (wersja z adnotacja, jesli istnieje,
 * w przeciwnym razie oryginal), plakietka "Adnotowane" i pobranie dokladnie tej wersji. Edycja - w karcie drzewa.
 */
export function DocumentationPhotoCard({ projectId, projectPhoto }: { projectId: number; projectPhoto: ProjectPhoto }) {
  const objectUrlState = useTreePhotoObjectUrl(projectId, projectPhoto.treeId, projectPhoto.photoId, 'documentation',
    projectPhoto.annotationUpdatedAt ?? null);
  const downloadFileName = projectPhoto.hasAnnotation
    ? `${projectPhoto.originalFileName.replace(/\.[^.]+$/, '')}-adnotacja.png`
    : projectPhoto.originalFileName;

  return (
    <li className="documentation-photo-card">
      <div className="tree-photo-thumbnail">
        {objectUrlState.status === 'loaded' && (
          <img src={objectUrlState.objectUrl} alt={`Drzewo ${projectPhoto.inventoryNumber}: ${projectPhoto.originalFileName}`}
               loading="lazy" />
        )}
        {objectUrlState.status === 'loading' && <LoadingSpinner />}
        {objectUrlState.status === 'failed' && <span className="muted small">Nie udało się wczytać zdjęcia.</span>}
      </div>
      {projectPhoto.hasAnnotation && <span className="badge tree-photo-annotated-badge">Adnotowane</span>}
      <div className="tree-photo-tile-footer">
        <span className="tree-photo-tile-name small" title={projectPhoto.originalFileName}>{projectPhoto.originalFileName}</span>
        {objectUrlState.status === 'loaded' && (
          <a className="btn btn-secondary btn-small" href={objectUrlState.objectUrl} download={downloadFileName}>Pobierz</a>
        )}
      </div>
    </li>
  );
}

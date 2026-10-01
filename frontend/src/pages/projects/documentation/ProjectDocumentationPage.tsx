import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { parsePositiveIdRouteParam } from '../../../routing/parsePositiveIdRouteParam';
import type { ProjectPhoto } from '../../trees/photos/treePhotoDataTypes';
import { treePhotosApi } from '../../trees/photos/treePhotosApi';
import { DocumentationPhotoCard } from './DocumentationPhotoCard';
import { groupProjectPhotosByTree } from './documentationPhotoGroups';
import { ProjectReportCard } from './ProjectReportCard';

type ProjectPhotosState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; projectPhotos: ProjectPhoto[] };

/**
 * Sekcja "Dokumentacja" workspace projektu (/projects/:projectId/documentation): "Dokument projektu" (PDF z aktualnych
 * danych, etap 22) oraz materialy do dokumentacji - zdjecia projektu pogrupowane po drzewie, w wersji do dokumentacji
 * (z adnotacja, jesli istnieje). Jedno zapytanie o liste zdjec projektu.
 */
export function ProjectDocumentationPage() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  const [projectPhotosState, setProjectPhotosState] = useState<ProjectPhotosState>({ status: 'loading' });

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    setProjectPhotosState({ status: 'loading' });
    treePhotosApi.listProjectPhotos(projectId)
      .then((projectPhotos) => {
        if (isResponseStillWanted) setProjectPhotosState({ status: 'loaded', projectPhotos });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setProjectPhotosState({
          status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.',
        });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  const photoGroups = useMemo(() => (projectPhotosState.status === 'loaded'
    ? groupProjectPhotosByTree(projectPhotosState.projectPhotos)
    : []), [projectPhotosState]);

  if (projectId === null) return null; // ProjectPage pokazuje juz stan "nie istnieje"

  if (projectPhotosState.status === 'loading') {
    return (
      <div className="stack">
        <h2>Dokumentacja</h2>
        <ProjectReportCard projectId={projectId} />
        <div className="center"><LoadingSpinner /></div>
      </div>
    );
  }

  if (projectPhotosState.status === 'failed') {
    return (
      <div className="stack">
        <h2>Dokumentacja</h2>
        <ProjectReportCard projectId={projectId} />
        <h3>Materiały do dokumentacji</h3>
        <Alert variant="error">Nie udało się pobrać zdjęć projektu. {projectPhotosState.errorMessage}</Alert>
      </div>
    );
  }

  const photoCount = projectPhotosState.projectPhotos.length;
  const annotatedPhotoCount = photoGroups.reduce((count, group) => count + group.annotatedPhotoCount, 0);

  return (
    <div className="stack">
      <h2>Dokumentacja</h2>
      <ProjectReportCard projectId={projectId} />
      <h3 className="documentation-materials-heading">Materiały do dokumentacji</h3>
      <p className="muted">
        Zdjęcia drzew w wersji do dokumentacji - jeżeli zdjęcie ma adnotację, widać wersję z naniesionym szkicem.
        Te same wersje trafiają do dokumentu PDF.
      </p>
      {photoCount === 0 ? (
        <div className="empty-state">
          <p>W tym projekcie nie ma jeszcze zdjęć. Dodaj je w karcie drzewa.</p>
          <Link to={`/projects/${projectId}/trees`} className="btn btn-primary">Przejdź do drzew</Link>
        </div>
      ) : (
        <>
          <p className="small muted">Zdjęcia: {photoCount}, w tym adnotowane: {annotatedPhotoCount}.</p>
          <ul className="documentation-tree-list">
            {photoGroups.map((group) => (
              <li key={group.treeId} className="documentation-tree-group">
                <div className="documentation-tree-group-header">
                  <h3>Drzewo nr {group.inventoryNumber}</h3>
                  <Link to={`/projects/${projectId}/trees/${group.treeId}`}>Przejdź do drzewa</Link>
                </div>
                <ul className="tree-photo-grid">
                  {group.photos.map((projectPhoto) => (
                    <DocumentationPhotoCard key={projectPhoto.photoId} projectId={projectId} projectPhoto={projectPhoto} />
                  ))}
                </ul>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}

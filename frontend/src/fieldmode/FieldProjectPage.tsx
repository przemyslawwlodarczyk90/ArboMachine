import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { Alert } from '../components/Alert';
import { LoadingSpinner } from '../components/LoadingSpinner';
import type { Project } from '../pages/projects/projectDataTypes';
import { projectsApi } from '../pages/projects/projectsApi';
import { parsePositiveIdRouteParam } from '../routing/parsePositiveIdRouteParam';
import { useFieldTopBar } from './FieldLayout';

type ProjectLoadingState =
  | { status: 'loading' }
  | { status: 'notFoundOrNoAccess' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; project: Project };

/** Mapa projektu w trybie terenowym (/field/projects/:projectId?drzewo=ID) - mapa na caly ekran + panel drzewa. */
export function FieldProjectPage() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  const [projectLoadingState, setProjectLoadingState] = useState<ProjectLoadingState>({ status: 'loading' });
  const setTopBarContent = useFieldTopBar();

  useEffect(() => {
    if (projectId === null) {
      setProjectLoadingState({ status: 'notFoundOrNoAccess' });
      return;
    }
    let isResponseStillWanted = true;
    projectsApi.getProjectOfCurrentUser(projectId)
      .then((project) => { if (isResponseStillWanted) setProjectLoadingState({ status: 'loaded', project }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        const isNotFound = loadingError instanceof ApiRequestError && (loadingError.httpStatus === 404 || loadingError.httpStatus === 403);
        setProjectLoadingState(isNotFound ? { status: 'notFoundOrNoAccess' }
          : { status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  const projectName = projectLoadingState.status === 'loaded' ? projectLoadingState.project.name : null;
  useEffect(() => {
    setTopBarContent({ title: projectName, showsBackLink: true, gpsIndicator: null });
  }, [setTopBarContent, projectName]);

  if (projectLoadingState.status === 'loading') return <div className="center field-padded"><LoadingSpinner /></div>;
  if (projectLoadingState.status === 'notFoundOrNoAccess') {
    return <div className="field-padded"><p>Ten projekt nie istnieje albo nie masz do niego dostępu.</p></div>;
  }
  if (projectLoadingState.status === 'failed') {
    return <div className="field-padded"><Alert variant="error">Nie udało się pobrać projektu. {projectLoadingState.errorMessage}</Alert></div>;
  }
  return <div className="field-map-screen" />;
}

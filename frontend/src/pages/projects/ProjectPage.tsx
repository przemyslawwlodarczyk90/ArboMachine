import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import type { Project } from './projectDataTypes';
import { projectsApi } from './projectsApi';

type ProjectLoadingState =
  | { status: 'loading' }
  | { status: 'loaded'; project: Project }
  /** 404 z backendu: projekt nie istnieje ALBO nalezy do kogos innego - celowo nierozroznialne. */
  | { status: 'notFoundOrNoAccess' }
  | { status: 'failed'; errorMessage: string };

/** Sekcje workspace - kazda to osobna trasa zagniezdzona, pod ktora pozniej wejdzie jej modul. */
const WORKSPACE_SECTION_LINKS = [
  { relativePath: '', label: 'Trees', matchExactPathOnly: true },
  { relativePath: 'map', label: 'Map', matchExactPathOnly: false },
  { relativePath: 'documentation', label: 'Photos & documentation', matchExactPathOnly: false },
];

/** Id projektu z adresu; cokolwiek innego niz dodatnia liczba calkowita traktujemy jak nieistniejacy projekt. */
function parseProjectIdFromRoute(projectIdRouteParam: string | undefined): number | null {
  if (!projectIdRouteParam || !/^\d+$/.test(projectIdRouteParam)) return null;
  const parsedProjectId = Number(projectIdRouteParam);
  return Number.isSafeInteger(parsedProjectId) && parsedProjectId > 0 ? parsedProjectId : null;
}

/**
 * Workspace projektu (/projects/:projectId): naglowek projektu + nawigacja sekcji + <Outlet/> na tresc sekcji.
 * Wlasnosci nie sprawdzamy w React - backend odpowiada 404 dla cudzego projektu.
 */
export function ProjectPage() {
  const { projectId: projectIdRouteParam } = useParams();
  const projectId = parseProjectIdFromRoute(projectIdRouteParam);
  const [projectLoadingState, setProjectLoadingState] = useState<ProjectLoadingState>({ status: 'loading' });

  useEffect(() => {
    if (projectId === null) {
      setProjectLoadingState({ status: 'notFoundOrNoAccess' });
      return;
    }
    let isResponseStillWanted = true;
    setProjectLoadingState({ status: 'loading' });
    projectsApi.getProjectOfCurrentUser(projectId)
      .then((project) => {
        if (isResponseStillWanted) setProjectLoadingState({ status: 'loaded', project });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        const isNotFoundOrForbidden = loadingError instanceof ApiRequestError
          && (loadingError.httpStatus === 404 || loadingError.httpStatus === 403);
        setProjectLoadingState(isNotFoundOrForbidden
          ? { status: 'notFoundOrNoAccess' }
          : { status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Unexpected error.' });
      });
    // Zmiana projectId w adresie (albo odmontowanie) uniewaznia odpowiedz na poprzednie zapytanie.
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  const backToProjectsLink = <Link to="/projects" className="back-link">← All projects</Link>;

  if (projectLoadingState.status === 'loading') {
    return <div className="center"><LoadingSpinner /></div>;
  }

  if (projectLoadingState.status === 'notFoundOrNoAccess') {
    return (
      <div className="stack-lg">
        {backToProjectsLink}
        <section className="card empty-state">
          <p>This project does not exist or you do not have access to it.</p>
          <Link to="/projects" className="btn btn-primary">Back to projects</Link>
        </section>
      </div>
    );
  }

  if (projectLoadingState.status === 'failed') {
    return (
      <div className="stack-lg">
        {backToProjectsLink}
        <Alert variant="error">We could not load this project. {projectLoadingState.errorMessage}</Alert>
      </div>
    );
  }

  const { project } = projectLoadingState;
  return (
    <div className="stack-lg">
      <header className="page-header">
        {backToProjectsLink}
        <h1>{project.name}</h1>
        {project.description && <p className="muted project-description">{project.description}</p>}
      </header>

      <nav className="workspace-tabs" aria-label="Project sections">
        {WORKSPACE_SECTION_LINKS.map((sectionLink) => (
          <NavLink key={sectionLink.label} to={sectionLink.relativePath} end={sectionLink.matchExactPathOnly}>
            {sectionLink.label}
          </NavLink>
        ))}
      </nav>

      <section className="card workspace-section">
        <Outlet />
      </section>
    </div>
  );
}

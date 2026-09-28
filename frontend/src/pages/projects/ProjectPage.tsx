import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useMatch, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { parsePositiveIdRouteParam } from '../../routing/parsePositiveIdRouteParam';
import { EditProjectDialog } from './EditProjectDialog';
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
  // Lista drzew to strona glowna projektu, ale zakladka ma byc aktywna takze na stronie drzewa (trees/:treeId).
  { relativePath: '', label: 'Trees', matchExactPathOnly: true, alsoActiveOnTreeDetailRoute: true },
  { relativePath: 'map', label: 'Map', matchExactPathOnly: false },
  { relativePath: 'documentation', label: 'Photos & documentation', matchExactPathOnly: false },
];

/**
 * Workspace projektu (/projects/:projectId): naglowek projektu + nawigacja sekcji + <Outlet/> na tresc sekcji.
 * Wlasnosci nie sprawdzamy w React - backend odpowiada 404 dla cudzego projektu.
 */
export function ProjectPage() {
  const { projectId: projectIdRouteParam } = useParams();
  const projectId = parsePositiveIdRouteParam(projectIdRouteParam);
  // Hook wywolywany przed wczesnymi returnami ponizej (zasady hookow Reacta).
  const isTreeDetailRoute = useMatch('/projects/:projectId/trees/*') !== null;
  const [projectLoadingState, setProjectLoadingState] = useState<ProjectLoadingState>({ status: 'loading' });
  const [isEditProjectDialogOpen, setIsEditProjectDialogOpen] = useState(false);

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
        <div className="page-header-row">
          <h1>{project.name}</h1>
          <button className="btn btn-secondary" onClick={() => setIsEditProjectDialogOpen(true)}>Edit project</button>
        </div>
        {project.description && <p className="muted project-description">{project.description}</p>}
      </header>

      <nav className="workspace-tabs" aria-label="Project sections">
        {WORKSPACE_SECTION_LINKS.map((sectionLink) => (
          <NavLink key={sectionLink.label} to={sectionLink.relativePath} end={sectionLink.matchExactPathOnly}
                   className={({ isActive }) =>
                     isActive || (sectionLink.alsoActiveOnTreeDetailRoute && isTreeDetailRoute) ? 'active' : undefined}>
            {sectionLink.label}
          </NavLink>
        ))}
      </nav>

      <section className="card workspace-section">
        <Outlet />
      </section>

      {isEditProjectDialogOpen && (
        <EditProjectDialog savedProject={project} closeDialog={() => setIsEditProjectDialogOpen(false)}
                           onProjectSaved={(savedProject) => {
                             setProjectLoadingState({ status: 'loaded', project: savedProject });
                             setIsEditProjectDialogOpen(false);
                           }} />
      )}
    </div>
  );
}

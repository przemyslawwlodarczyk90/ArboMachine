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

/**
 * Sekcje workspace - kazda to osobna trasa zagniezdzona. Podsumowanie to strona glowna projektu; zakladka "Drzewa"
 * (trees) jest aktywna takze na karcie drzewa (trees/:treeId), bo NavLink dopasowuje prefiks sciezki.
 */
const WORKSPACE_SECTION_LINKS = [
  { relativePath: '', label: 'Podsumowanie', matchExactPathOnly: true },
  { relativePath: 'trees', label: 'Drzewa', matchExactPathOnly: false },
  { relativePath: 'map', label: 'Mapa', matchExactPathOnly: false },
  { relativePath: 'documentation', label: 'Dokumentacja', matchExactPathOnly: false },
  { relativePath: 'import-export', label: 'Import i eksport', matchExactPathOnly: false },
];

/**
 * Workspace projektu (/projects/:projectId): naglowek projektu + nawigacja sekcji + <Outlet/> na tresc sekcji.
 * Wlasnosci nie sprawdzamy w React - backend odpowiada 404 dla cudzego projektu.
 */
export function ProjectPage() {
  const { projectId: projectIdRouteParam } = useParams();
  const projectId = parsePositiveIdRouteParam(projectIdRouteParam);
  // Hook wywolywany przed wczesnymi returnami ponizej (zasady hookow Reacta). Na karcie drzewa breadcrumb
  // prowadzi dodatkowo do listy drzew; numer drzewa jest w naglowku karty.
  const isTreeDetailRoute = useMatch('/projects/:projectId/trees/:treeId') !== null;
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
          : { status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    // Zmiana projectId w adresie (albo odmontowanie) uniewaznia odpowiedz na poprzednie zapytanie.
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  const backToProjectsLink = <Link to="/projects" className="back-link">← Wszystkie projekty</Link>;

  if (projectLoadingState.status === 'loading') {
    return <div className="center"><LoadingSpinner /></div>;
  }

  if (projectLoadingState.status === 'notFoundOrNoAccess') {
    return (
      <div className="stack-lg">
        {backToProjectsLink}
        <section className="card empty-state">
          <p>Ten projekt nie istnieje albo nie masz do niego dostępu.</p>
          <Link to="/projects" className="btn btn-primary">Wróć do projektów</Link>
        </section>
      </div>
    );
  }

  if (projectLoadingState.status === 'failed') {
    return (
      <div className="stack-lg">
        {backToProjectsLink}
        <Alert variant="error">Nie udało się wczytać projektu. {projectLoadingState.errorMessage}</Alert>
      </div>
    );
  }

  const { project } = projectLoadingState;
  return (
    <div className="stack-lg">
      <header className="page-header">
        {/* Prosty breadcrumb: zawsze widac, w jakim projekcie jestesmy i jak wrocic do listy projektow. */}
        <nav className="breadcrumb" aria-label="Ścieżka nawigacji">
          <Link to="/projects">Projekty</Link>
          <span aria-hidden="true">/</span>
          <Link to={`/projects/${project.id}`}>{project.name}</Link>
          {isTreeDetailRoute && (
            <>
              <span aria-hidden="true">/</span>
              <Link to={`/projects/${project.id}/trees`}>Drzewa</Link>
            </>
          )}
        </nav>
        <div className="page-header-row">
          <h1>{project.name}</h1>
          <button className="btn btn-secondary" onClick={() => setIsEditProjectDialogOpen(true)}>Edytuj projekt</button>
        </div>
        {project.description && <p className="muted project-description">{project.description}</p>}
      </header>

      <nav className="workspace-tabs" aria-label="Sekcje projektu">
        {WORKSPACE_SECTION_LINKS.map((sectionLink) => (
          <NavLink key={sectionLink.label} to={sectionLink.relativePath} end={sectionLink.matchExactPathOnly}
                   className={({ isActive }) => (isActive ? 'active' : undefined)}>
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

import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { Alert } from '../components/Alert';
import { LoadingSpinner } from '../components/LoadingSpinner';
import type { Project } from '../pages/projects/projectDataTypes';
import { projectsApi } from '../pages/projects/projectsApi';
import { useFieldTopBar } from './FieldLayout';
import { fieldProjectPath } from './fieldModePaths';
import { rememberWorkMode } from './fieldModePreference';

type ProjectListState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; projects: Project[] };

/**
 * Lista projektow w trybie terenowym (/field): te same dane co lista biurowa, duze wiersze do dotyku.
 * Bez tworzenia i edycji projektu - to praca biurowa (link do widoku biurowego).
 */
export function FieldProjectsPage() {
  const [projectListState, setProjectListState] = useState<ProjectListState>({ status: 'loading' });
  const setTopBarContent = useFieldTopBar();

  useEffect(() => {
    setTopBarContent({ title: 'Projekty', showsBackLink: false, gpsIndicator: null });
  }, [setTopBarContent]);

  useEffect(() => {
    let isResponseStillWanted = true;
    projectsApi.listProjectsOfCurrentUser()
      .then((projects) => { if (isResponseStillWanted) setProjectListState({ status: 'loaded', projects }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setProjectListState({ status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, []);

  return (
    <div className="field-projects stack">
      {projectListState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}
      {projectListState.status === 'failed' && (
        <Alert variant="error">Nie udało się wczytać projektów. {projectListState.errorMessage}</Alert>
      )}
      {projectListState.status === 'loaded' && projectListState.projects.length === 0 && (
        <p className="muted">Nie masz jeszcze żadnych projektów.</p>
      )}
      {projectListState.status === 'loaded' && projectListState.projects.length > 0 && (
        <ul className="field-project-list">
          {projectListState.projects.map((project) => (
            <li key={project.id}>
              <Link to={fieldProjectPath(project.id)} className="field-project-row">
                <span className="field-project-row-name">{project.name}</span>
                {(project.siteAddress || project.clientName) && (
                  <span className="muted small">{[project.siteAddress, project.clientName].filter(Boolean).join(' · ')}</span>
                )}
              </Link>
            </li>
          ))}
        </ul>
      )}
      <Link to="/projects" className="btn btn-ghost field-projects-office-link" onClick={() => rememberWorkMode('OFFICE')}>
        Nowy projekt w widoku biurowym
      </Link>
    </div>
  );
}

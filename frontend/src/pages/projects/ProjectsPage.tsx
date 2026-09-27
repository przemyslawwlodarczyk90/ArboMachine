import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { formatProjectTimestamp } from './formatProjectTimestamp';
import { NewProjectDialog } from './NewProjectDialog';
import type { Project } from './projectDataTypes';
import { projectsApi } from './projectsApi';

/** Stan listy - "loading" i "empty" sa rozdzielone, zeby przed koncem zapytania nie mignal pusty stan. */
type ProjectListState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; projects: Project[] };

/** Glowny ekran projektow arborysty: lista wlasnych projektow i tworzenie nowego. */
export function ProjectsPage() {
  const [projectListState, setProjectListState] = useState<ProjectListState>({ status: 'loading' });
  const [isNewProjectDialogOpen, setIsNewProjectDialogOpen] = useState(false);

  useEffect(() => {
    let isResponseStillWanted = true;
    projectsApi.listProjectsOfCurrentUser()
      .then((projects) => {
        if (isResponseStillWanted) setProjectListState({ status: 'loaded', projects });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setProjectListState({
          status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Unexpected error.',
        });
      });
    return () => { isResponseStillWanted = false; };
  }, []);

  const openNewProjectDialog = () => setIsNewProjectDialogOpen(true);

  return (
    <div className="stack-lg">
      <header className="page-header page-header-row">
        <h1>Projects</h1>
        <button className="btn btn-primary" onClick={openNewProjectDialog}>New project</button>
      </header>

      {projectListState.status === 'loading' && (
        <div className="center"><LoadingSpinner /></div>
      )}

      {projectListState.status === 'failed' && (
        <Alert variant="error">We could not load your projects. {projectListState.errorMessage}</Alert>
      )}

      {projectListState.status === 'loaded' && projectListState.projects.length === 0 && (
        <section className="card empty-state">
          <p>You don't have any projects yet.</p>
          <button className="btn btn-primary" onClick={openNewProjectDialog}>Create your first project</button>
        </section>
      )}

      {projectListState.status === 'loaded' && projectListState.projects.length > 0 && (
        <ul className="project-list">
          {projectListState.projects.map((project) => (
            <li key={project.id}>
              <Link to={`/projects/${project.id}`} className="card project-list-item">
                <span className="project-list-item-name">{project.name}</span>
                {project.description && <span className="project-list-item-description">{project.description}</span>}
                <span className="muted small">
                  Created {formatProjectTimestamp(project.createdAt)}
                  {project.updatedAt !== project.createdAt && <> · Updated {formatProjectTimestamp(project.updatedAt)}</>}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {isNewProjectDialogOpen && <NewProjectDialog closeDialog={() => setIsNewProjectDialogOpen(false)} />}
    </div>
  );
}

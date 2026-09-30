import { Navigate, useParams } from 'react-router-dom';

/** Przekierowanie starej trasy projektu (np. /projects/:projectId/documentation) na podsumowanie projektu. */
export function RedirectToProjectSummary() {
  const { projectId } = useParams();
  return <Navigate to={`/projects/${projectId ?? ''}`} replace />;
}

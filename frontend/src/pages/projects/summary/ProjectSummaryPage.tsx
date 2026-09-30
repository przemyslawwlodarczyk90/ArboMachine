import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { parsePositiveIdRouteParam } from '../../../routing/parsePositiveIdRouteParam';
import type { ProjectSummary } from './projectSummaryDataTypes';
import { projectSummaryApi } from './projectSummaryApi';
import { toProjectSummaryStatistics } from './projectSummaryStatistics';

type ProjectSummaryState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; projectSummary: ProjectSummary };

/**
 * Sekcja "Podsumowanie" workspace projektu (/projects/:projectId) - ekran roboczy, a nie dashboard analityczny:
 * kilka liczb liczonych w backendzie i szybkie przejscia do istniejacych funkcji. Liczby pobierane sa przy kazdym
 * wejsciu na sekcje, wiec po zmianach w drzewach sa aktualne. Renderuje sie dopiero po udanym pobraniu projektu
 * (ProjectPage), wiec cudzy projekt tu nie dotrze.
 */
export function ProjectSummaryPage() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  const [projectSummaryState, setProjectSummaryState] = useState<ProjectSummaryState>({ status: 'loading' });
  const [loadAttempt, setLoadAttempt] = useState(0);

  useEffect(() => {
    if (projectId === null) return;
    let isResponseStillWanted = true;
    setProjectSummaryState({ status: 'loading' });
    projectSummaryApi.getProjectSummary(projectId)
      .then((projectSummary) => {
        if (isResponseStillWanted) setProjectSummaryState({ status: 'loaded', projectSummary });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setProjectSummaryState({
          status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.',
        });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId, loadAttempt]);

  const retryLoading = useCallback(() => setLoadAttempt((attempt) => attempt + 1), []);

  if (projectId === null) return null; // ProjectPage pokazuje juz stan "nie istnieje"

  const treesPath = `/projects/${projectId}/trees`;
  const mapPath = `/projects/${projectId}/map`;

  return (
    <div className="stack-lg">
      <h2>Podsumowanie projektu</h2>

      {projectSummaryState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}

      {projectSummaryState.status === 'failed' && (
        <div className="stack">
          <Alert variant="error">Nie udało się pobrać podsumowania projektu. {projectSummaryState.errorMessage}</Alert>
          <div><button className="btn btn-secondary" onClick={retryLoading}>Spróbuj ponownie</button></div>
        </div>
      )}

      {projectSummaryState.status === 'loaded' && (
        <>
          <ul className="summary-statistics" aria-label="Liczby w projekcie">
            {toProjectSummaryStatistics(projectSummaryState.projectSummary).map((statistic) => (
              <li key={statistic.label} className="summary-statistic">
                <span className="summary-statistic-value">{statistic.value}</span>
                <span className="summary-statistic-label">{statistic.label}</span>
              </li>
            ))}
          </ul>

          {projectSummaryState.projectSummary.treeCount === 0 && (
            <Alert variant="info">Projekt nie zawiera jeszcze żadnych drzew.</Alert>
          )}
          {projectSummaryState.projectSummary.treeWithoutLocationCount > 0 && (
            <p className="muted">
              Drzewa bez lokalizacji: {projectSummaryState.projectSummary.treeWithoutLocationCount}. Lokalizację
              ustawisz w karcie drzewa (sekcja „Lokalizacja”).
            </p>
          )}

          <section className="stack" aria-labelledby="quick-actions-heading">
            <h3 id="quick-actions-heading">Szybkie akcje</h3>
            <div className="quick-actions">
              <Link to={treesPath} className="btn btn-primary">
                {projectSummaryState.projectSummary.treeCount === 0 ? 'Przejdź do drzew i dodaj pierwsze' : 'Przejdź do drzew'}
              </Link>
              <Link to={mapPath} className="btn btn-secondary">Otwórz mapę</Link>
            </div>
          </section>
        </>
      )}
    </div>
  );
}

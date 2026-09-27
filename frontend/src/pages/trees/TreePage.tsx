import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { parsePositiveIdRouteParam } from '../../routing/parsePositiveIdRouteParam';
import type { Tree } from './treeDataTypes';
import { treesApi } from './treesApi';

type TreeLoadingState =
  | { status: 'loading' }
  | { status: 'loaded'; tree: Tree }
  /** 404 z backendu (drzewo nie istnieje, jest w innym projekcie albo projekt nie nalezy do uzytkownika). */
  | { status: 'notFoundOrNoAccess' }
  | { status: 'failed'; errorMessage: string };

/**
 * Minimalny podglad drzewa (/projects/:projectId/trees/:treeId) - dowod dzialajacego pionu
 * Project -> Tree -> szczegoly, jeszcze NIE docelowa karta drzewa. Wlasnosci nie sprawdzamy w React.
 */
export function TreePage() {
  const routeParams = useParams();
  const projectId = parsePositiveIdRouteParam(routeParams.projectId);
  const treeId = parsePositiveIdRouteParam(routeParams.treeId);
  const [treeLoadingState, setTreeLoadingState] = useState<TreeLoadingState>({ status: 'loading' });

  useEffect(() => {
    if (projectId === null || treeId === null) {
      setTreeLoadingState({ status: 'notFoundOrNoAccess' });
      return;
    }
    let isResponseStillWanted = true;
    setTreeLoadingState({ status: 'loading' });
    treesApi.getTreeOfProject(projectId, treeId)
      .then((tree) => {
        if (isResponseStillWanted) setTreeLoadingState({ status: 'loaded', tree });
      })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        const isNotFoundOrForbidden = loadingError instanceof ApiRequestError
          && (loadingError.httpStatus === 404 || loadingError.httpStatus === 403);
        setTreeLoadingState(isNotFoundOrForbidden
          ? { status: 'notFoundOrNoAccess' }
          : { status: 'failed', errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Unexpected error.' });
      });
    return () => { isResponseStillWanted = false; };
  }, [projectId, treeId]);

  const backToTreesLink = <Link to={`/projects/${projectId ?? ''}`} className="back-link">← Back to trees</Link>;

  if (treeLoadingState.status === 'loading') {
    return <div className="center"><LoadingSpinner /></div>;
  }

  if (treeLoadingState.status === 'notFoundOrNoAccess') {
    return (
      <div className="stack">
        {backToTreesLink}
        <div className="empty-state">
          <p>This tree does not exist or you do not have access to it.</p>
        </div>
      </div>
    );
  }

  if (treeLoadingState.status === 'failed') {
    return (
      <div className="stack">
        {backToTreesLink}
        <Alert variant="error">We could not load this tree. {treeLoadingState.errorMessage}</Alert>
      </div>
    );
  }

  const { tree } = treeLoadingState;
  return (
    <div className="stack">
      {backToTreesLink}
      <h2>Tree {tree.inventoryNumber}</h2>
      <dl className="tree-details">
        <dt>Latin species</dt>
        <dd>{tree.speciesLatin || <span className="muted">Not specified</span>}</dd>
        <dt>Notes</dt>
        <dd className="tree-details-notes">{tree.notes || <span className="muted">No notes</span>}</dd>
      </dl>
    </div>
  );
}

import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { parsePositiveIdRouteParam } from '../../routing/parsePositiveIdRouteParam';
import { TreeDetailsView } from './TreeDetailsView';
import { TreeEditForm } from './TreeEditForm';
import type { Tree } from './treeDataTypes';
import { treesApi } from './treesApi';

type TreeLoadingState =
  | { status: 'loading' }
  | { status: 'loaded'; tree: Tree }
  /** 404 z backendu (drzewo nie istnieje, jest w innym projekcie albo projekt nie nalezy do uzytkownika). */
  | { status: 'notFoundOrNoAccess' }
  | { status: 'failed'; errorMessage: string };

/**
 * Karta drzewa (/projects/:projectId/trees/:treeId). Strona tylko orkiestruje: laduje drzewo
 * i przelacza tryb podgladu (TreeDetailsView) / edycji (TreeEditForm). Wlasnosci nie sprawdzamy w React.
 */
export function TreePage() {
  const routeParams = useParams();
  const projectId = parsePositiveIdRouteParam(routeParams.projectId);
  const treeId = parsePositiveIdRouteParam(routeParams.treeId);
  const [treeLoadingState, setTreeLoadingState] = useState<TreeLoadingState>({ status: 'loading' });
  const [isEditingTree, setIsEditingTree] = useState(false);

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
  // Po zapisie podmieniamy drzewo odpowiedzia PATCH (bez dodatkowego GET) i wracamy do podgladu.
  const replaceTreeWithSavedOne = (savedTree: Tree) => {
    setTreeLoadingState({ status: 'loaded', tree: savedTree });
    setIsEditingTree(false);
  };

  return (
    <div className="stack">
      {backToTreesLink}
      {isEditingTree && projectId !== null
        ? <TreeEditForm projectId={projectId} savedTree={tree} onTreeSaved={replaceTreeWithSavedOne}
                        cancelEditing={() => setIsEditingTree(false)} />
        : <TreeDetailsView tree={tree} startEditing={() => setIsEditingTree(true)} />}
    </div>
  );
}

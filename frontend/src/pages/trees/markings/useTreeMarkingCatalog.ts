import { useEffect, useMemo, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import type { TreeMarkingDefinition } from './treeMarkingDataTypes';
import { treeMarkingsApi } from './treeMarkingsApi';

export type TreeMarkingCatalogState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; markings: TreeMarkingDefinition[] };

/** Katalog oznaczen (raz na sesje strony) + mapa kod -> oznaczenie. */
export function useTreeMarkingCatalog() {
  const [catalogState, setCatalogState] = useState<TreeMarkingCatalogState>({ status: 'loading' });

  useEffect(() => {
    let isResponseStillWanted = true;
    treeMarkingsApi.getCatalog()
      .then((markings) => { if (isResponseStillWanted) setCatalogState({ status: 'loaded', markings }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setCatalogState({ status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, []);

  const markingByCode = useMemo(() => new Map(
    (catalogState.status === 'loaded' ? catalogState.markings : []).map((marking) => [marking.code, marking])), [catalogState]);
  return { catalogState, markingByCode };
}

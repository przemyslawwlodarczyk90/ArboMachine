import { useEffect, useMemo, useState } from 'react';
import { ApiRequestError } from '../api/httpClient';
import type { MapSymbol } from './mapSymbolDataTypes';
import { mapSymbolsApi } from './mapSymbolsApi';

export type MapSymbolCatalogState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'loaded'; symbols: MapSymbol[] };

/** Katalog symboli (pobrany raz na sesje strony) + mapa kod -> symbol do szybkiego wyszukiwania. */
export function useMapSymbolCatalog() {
  const [catalogState, setCatalogState] = useState<MapSymbolCatalogState>({ status: 'loading' });

  useEffect(() => {
    let isResponseStillWanted = true;
    mapSymbolsApi.getMapSymbols()
      .then((symbols) => { if (isResponseStillWanted) setCatalogState({ status: 'loaded', symbols }); })
      .catch((loadingError) => {
        if (!isResponseStillWanted) return;
        setCatalogState({ status: 'failed',
          errorMessage: loadingError instanceof ApiRequestError ? loadingError.message : 'Nieoczekiwany błąd.' });
      });
    return () => { isResponseStillWanted = false; };
  }, []);

  const symbolByCode = useMemo(() => new Map(
    (catalogState.status === 'loaded' ? catalogState.symbols : []).map((symbol) => [symbol.code, symbol])), [catalogState]);
  return { catalogState, symbolByCode };
}

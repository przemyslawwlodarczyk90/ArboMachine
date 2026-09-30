import { sendApiRequest } from '../api/httpClient';
import type { MapSymbol } from './mapSymbolDataTypes';

/**
 * Katalog symboli jest staly w czasie dzialania backendu, wiec wystarczy go pobrac raz na sesje strony.
 * Zapamietujemy obietnice (bez biblioteki cache); po bledzie zapominamy ja, zeby kolejne wejscie sprobowalo ponownie.
 */
let mapSymbolCatalogRequest: Promise<MapSymbol[]> | null = null;

export const mapSymbolsApi = {
  getMapSymbols: (): Promise<MapSymbol[]> => {
    if (!mapSymbolCatalogRequest) {
      mapSymbolCatalogRequest = sendApiRequest<MapSymbol[]>('/api/map-symbols', { sendAccessToken: true })
        .catch((loadingError) => {
          mapSymbolCatalogRequest = null;
          throw loadingError;
        });
    }
    return mapSymbolCatalogRequest;
  },
};

import { useRef, useState } from 'react';
import { ApiRequestError } from '../../api/httpClient';
import { Alert } from '../../components/Alert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { MapSymbolPicker } from '../../map/MapSymbolPicker';
import { MapSymbolSwatch } from '../../map/MapSymbolSwatch';
import { useMapSymbolCatalog } from '../../map/useMapSymbolCatalog';
import type { Tree } from './treeDataTypes';
import { treesApi } from './treesApi';

/**
 * Sekcja "Symbol drzewa na mapie" karty drzewa: wybor symbolu bazowego z katalogu, zapis i usuniecie.
 * To prezentacja mapy, a nie dane arborystyczne - osobna operacja (PUT/DELETE .../map-symbol), niezalezna od
 * formularza "Dane drzewa". Po zapisie drzewo z odpowiedzi backendu wraca do karty (jedno zrodlo prawdy).
 */
export function TreeMapSymbolSection({ projectId, tree, onTreeChanged }: {
  projectId: number;
  tree: Tree;
  onTreeChanged: (changedTree: Tree) => void;
}) {
  const { catalogState, symbolByCode } = useMapSymbolCatalog();
  const savedSymbolCode = tree.mapSymbolCode ?? null;
  const [selectedSymbolCode, setSelectedSymbolCode] = useState<string | null>(savedSymbolCode);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);
  const savedSymbol = savedSymbolCode ? symbolByCode.get(savedSymbolCode) : undefined;

  const saveSymbol = async (symbolCode: string | null) => {
    if (isSubmissionInProgress.current) return;
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    setErrorMessage(null);
    try {
      const changedTree = symbolCode
        ? await treesApi.setTreeMapSymbol(projectId, tree.id, symbolCode)
        : await treesApi.clearTreeMapSymbol(projectId, tree.id);
      setSelectedSymbolCode(changedTree.mapSymbolCode ?? null);
      onTreeChanged(changedTree);
    } catch (savingError) {
      setErrorMessage(`Nie udało się zapisać symbolu. ${savingError instanceof ApiRequestError ? savingError.message : 'Nieoczekiwany błąd.'}`);
    } finally {
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  return (
    <section className="tree-card-section" aria-labelledby="tree-map-symbol-heading">
      <div className="section-header-row">
        <h3 id="tree-map-symbol-heading">Symbol drzewa na mapie</h3>
      </div>
      <p className="muted small">
        Aktualnie: {savedSymbol
          ? <span className="map-symbol-inline"><MapSymbolSwatch symbol={savedSymbol} size={18} /> {savedSymbol.displayName}</span>
          : 'brak symbolu (neutralny punkt)'}
      </p>
      {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
      {catalogState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}
      {catalogState.status === 'failed' && (
        <Alert variant="error">Nie udało się pobrać katalogu symboli. {catalogState.errorMessage}</Alert>
      )}
      {catalogState.status === 'loaded' && (
        <>
          <MapSymbolPicker symbols={catalogState.symbols} selectedSymbolCode={selectedSymbolCode}
                           onSelect={setSelectedSymbolCode} disabled={isSaving} groupLabel="Symbol drzewa na mapie" />
          <div className="form-actions">
            {savedSymbolCode && (
              <button type="button" className="btn btn-danger-outline" disabled={isSaving} onClick={() => saveSymbol(null)}>
                Usuń symbol
              </button>
            )}
            <button type="button" className="btn btn-primary"
                    disabled={isSaving || !selectedSymbolCode || selectedSymbolCode === savedSymbolCode}
                    onClick={() => saveSymbol(selectedSymbolCode)}>
              {isSaving ? 'Zapisywanie…' : 'Zapisz symbol'}
            </button>
          </div>
        </>
      )}
    </section>
  );
}

import { useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import type { MapSymbol } from '../../../map/mapSymbolDataTypes';
import { MapSymbolPicker } from '../../../map/MapSymbolPicker';
import type { TreeWork } from './treeWorkDataTypes';
import { treeWorksApi } from './treeWorksApi';

/**
 * Zmiana symbolu jednej pracy na mapie - bez edycji nazwy i opisu (osobne PUT/DELETE .../map-symbol).
 * Po sukcesie oddaje prace z odpowiedzi backendu; przy bledzie zostaje otwarty z wybranym symbolem.
 */
export function TreeWorkMapSymbolEditor({ projectId, treeId, treeWork, symbols, onTreeWorkChanged, closeEditor }: {
  projectId: number;
  treeId: number;
  treeWork: TreeWork;
  symbols: MapSymbol[];
  onTreeWorkChanged: (changedTreeWork: TreeWork) => void;
  closeEditor: () => void;
}) {
  const savedSymbolCode = treeWork.mapSymbolCode ?? null;
  const [selectedSymbolCode, setSelectedSymbolCode] = useState<string | null>(savedSymbolCode);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const isSubmissionInProgress = useRef(false);

  const saveSymbol = async (symbolCode: string | null) => {
    if (isSubmissionInProgress.current) return;
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    setErrorMessage(null);
    try {
      onTreeWorkChanged(symbolCode
        ? await treeWorksApi.setTreeWorkMapSymbol(projectId, treeId, treeWork.id, symbolCode)
        : await treeWorksApi.clearTreeWorkMapSymbol(projectId, treeId, treeWork.id));
    } catch (savingError) {
      setErrorMessage(`Nie udało się zapisać symbolu. ${savingError instanceof ApiRequestError ? savingError.message : 'Nieoczekiwany błąd.'}`);
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  return (
    <div className="tree-work-symbol-editor stack">
      {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
      <MapSymbolPicker symbols={symbols} selectedSymbolCode={selectedSymbolCode} onSelect={setSelectedSymbolCode}
                       disabled={isSaving} groupLabel={`Symbol na mapie: ${treeWork.name}`} />
      <div className="form-actions">
        <button type="button" className="btn btn-ghost" onClick={closeEditor} disabled={isSaving}>Anuluj</button>
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
    </div>
  );
}

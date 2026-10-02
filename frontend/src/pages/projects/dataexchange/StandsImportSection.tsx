import { useRef, useState, type ChangeEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { formatStandArea } from '../stands/standGeometry';
import type { TreeStandImportResult, TreeStandImportValidation } from './dataExchangeDataTypes';
import { dataExchangeApi } from './dataExchangeApi';
import { describeImportIssue } from './importIssueMessages';

type ImportState =
  | { status: 'idle' }
  | { status: 'validating' }
  | { status: 'validated'; validation: TreeStandImportValidation }
  | { status: 'importing'; validation: TreeStandImportValidation }
  | { status: 'imported'; result: TreeStandImportResult }
  | { status: 'failed'; errorMessage: string };

const errorText = (error: unknown) => (error instanceof ApiRequestError ? error.message : 'Nieoczekiwany błąd.');

/**
 * Import grup drzew (etap 29): obszary z rysunku DXF (warstwy obszarów), Shapefile ZIP, GeoJSON albo GeoPackage.
 * Jak import drzew: "Sprawdź plik" -> podsumowanie po polsku -> "Importuj grupy" (wszystko albo nic). Drzewa projektu
 * leżące w obszarze są przypisywane automatycznie - dlatego najpierw warto zaimportować drzewa.
 */
export function StandsImportSection({ projectId }: { projectId: number }) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [importState, setImportState] = useState<ImportState>({ status: 'idle' });
  const isRequestInProgress = useRef(false);

  const selectFile = (changeEvent: ChangeEvent<HTMLInputElement>) => {
    setSelectedFile(changeEvent.target.files?.[0] ?? null);
    setImportState({ status: 'idle' });
  };

  const validateFile = async () => {
    if (!selectedFile || isRequestInProgress.current) return;
    isRequestInProgress.current = true;
    setImportState({ status: 'validating' });
    try {
      setImportState({ status: 'validated', validation: await dataExchangeApi.validateStandImport(projectId, selectedFile) });
    } catch (validationError) {
      setImportState({ status: 'failed', errorMessage: `Nie udało się sprawdzić pliku. ${errorText(validationError)}` });
    } finally {
      isRequestInProgress.current = false;
    }
  };

  const importFile = async (validation: TreeStandImportValidation) => {
    if (!selectedFile || isRequestInProgress.current) return;
    isRequestInProgress.current = true;
    setImportState({ status: 'importing', validation });
    try {
      setImportState({ status: 'imported', result: await dataExchangeApi.importStands(projectId, selectedFile) });
    } catch (importError) {
      setImportState({ status: 'failed', errorMessage: importError instanceof ApiRequestError && importError.httpStatus === 400
        ? 'Plik zawiera błędy - nic nie zostało zaimportowane. Sprawdź plik ponownie.'
        : `Nie udało się zaimportować grup drzew - nic nie zostało zapisane. ${errorText(importError)}` });
    } finally {
      isRequestInProgress.current = false;
    }
  };

  const validation = importState.status === 'validated' || importState.status === 'importing' ? importState.validation : null;
  const isBusy = importState.status === 'validating' || importState.status === 'importing';
  const allErrors = validation ? [...validation.fileErrors, ...validation.errors] : [];

  return (
    <section className="data-exchange-card stack" aria-labelledby="stands-import-heading">
      <h3 id="stands-import-heading">Import grup drzew (obszarów)</h3>
      <p className="muted">
        Obszary (wielokąty) z rysunku DXF, Shapefile (ZIP), GeoJSON albo GeoPackage - każdy obszar staje się grupą drzew.
        Nazwa z atrybutu „name” / „nazwa” (inaczej „Grupa drzew N”). Drzewa projektu leżące w obszarze zostaną przypisane
        automatycznie - dlatego najpierw zaimportuj drzewa.
      </p>
      <div className="field">
        <label htmlFor="stands-import-file">Plik z obszarami</label>
        <input id="stands-import-file" type="file" accept=".dxf,.zip,.geojson,.json,.gpkg" onChange={selectFile} disabled={isBusy} />
        {selectedFile && <span className="field-hint">Wybrany plik: {selectedFile.name}</span>}
      </div>
      <div className="form-actions data-exchange-actions">
        <button type="button" className="btn btn-secondary" onClick={validateFile} disabled={!selectedFile || isBusy}>
          {importState.status === 'validating' ? 'Sprawdzanie…' : 'Sprawdź plik'}
        </button>
      </div>
      <div aria-live="polite" className="stack">
        {importState.status === 'failed' && <Alert variant="error">{importState.errorMessage}</Alert>}
        {importState.status === 'imported' && (
          <Alert variant="success">
            Zaimportowano grupy drzew: {importState.result.importedStandCount} (przypisane drzewa: {importState.result.assignedTreeCount}).{' '}
            <Link to={`/projects/${projectId}/stands`}>Przejdź do grup drzew</Link>
          </Alert>
        )}
        {validation && (
          <div className="stack data-exchange-validation">
            {validation.readyToImport
              ? <Alert variant="success">Plik jest poprawny. Można zaimportować grupy drzew: {validation.validStandCount}.</Alert>
              : <Alert variant="error">Plik zawiera błędy - import jest możliwy tylko bez błędów.</Alert>}
            {allErrors.length > 0 && (
              <ul className="data-exchange-issues">
                {allErrors.slice(0, 20).map((issue, index) => <li key={index}>{describeImportIssue(issue, validation.format)}</li>)}
              </ul>
            )}
            {validation.warnings.length > 0 && (
              <ul className="data-exchange-issues data-exchange-warnings">
                {validation.warnings.map((issue, index) => <li key={index}>{describeImportIssue(issue, validation.format)}</li>)}
              </ul>
            )}
            {validation.preview.length > 0 && (
              <div className="table-wrapper">
                <table>
                  <caption className="muted small">Podgląd pierwszych obszarów</caption>
                  <thead><tr><th>Obiekt</th><th>Nazwa</th><th>Powierzchnia</th><th>Drzewa w obszarze</th></tr></thead>
                  <tbody>
                    {validation.preview.map((stand) => (
                      <tr key={stand.rowNumber}>
                        <td className="num">{stand.rowNumber}</td>
                        <td>{stand.name}</td>
                        <td className="num">{formatStandArea(stand.areaSquareMeters)}</td>
                        <td className="num">{stand.treeCountInside}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {validation.readyToImport && (
              <div className="form-actions">
                <button type="button" className="btn btn-primary" onClick={() => void importFile(validation)} disabled={isBusy}>
                  {importState.status === 'importing' ? 'Importowanie…' : `Importuj grupy drzew (${validation.validStandCount})`}
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}

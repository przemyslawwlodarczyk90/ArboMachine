import { useRef, useState, type ChangeEvent } from 'react';
import { Link } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import type { TreeImportResult, TreeImportValidation } from './dataExchangeDataTypes';
import { dataExchangeApi } from './dataExchangeApi';
import { describeImportIssue } from './importIssueMessages';

type ImportState =
  | { status: 'idle' }
  | { status: 'validating' }
  | { status: 'validated'; validation: TreeImportValidation }
  | { status: 'importing'; validation: TreeImportValidation }
  | { status: 'imported'; result: TreeImportResult }
  | { status: 'failed'; errorMessage: string };

/** Ile bledow pokazujemy od razu - przy setkach bledow reszta jest zwinieta. */
const VISIBLE_ISSUE_COUNT = 20;

const formatNumber = (value: number | null) => (value === null ? '—' : String(value).replace('.', ','));

/**
 * Import drzew (CSV, XLSX, GeoJSON): wybierz plik -> "Sprawdz plik" (backend parsuje i waliduje, nic nie zapisuje)
 * -> podsumowanie, bledy i ostrzezenia po polsku -> "Importuj drzewa" (tylko gdy plik nie ma bledow - wszystko albo nic).
 * Kazdy wiersz tworzy NOWE drzewo.
 */
export function TreesImportSection({ projectId }: { projectId: number }) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [importState, setImportState] = useState<ImportState>({ status: 'idle' });
  const [areAllIssuesVisible, setAreAllIssuesVisible] = useState(false);
  const isRequestInProgress = useRef(false);

  const selectFile = (changeEvent: ChangeEvent<HTMLInputElement>) => {
    setSelectedFile(changeEvent.target.files?.[0] ?? null);
    setImportState({ status: 'idle' });
    setAreAllIssuesVisible(false);
  };

  const errorMessageOf = (requestError: unknown) =>
    requestError instanceof ApiRequestError ? requestError.message : 'Nieoczekiwany błąd.';

  const validateFile = async () => {
    if (!selectedFile || isRequestInProgress.current) return;
    isRequestInProgress.current = true;
    setImportState({ status: 'validating' });
    try {
      setImportState({ status: 'validated', validation: await dataExchangeApi.validateTreeImport(projectId, selectedFile) });
    } catch (validationError) {
      setImportState({ status: 'failed', errorMessage: `Nie udało się sprawdzić pliku. ${errorMessageOf(validationError)}` });
    } finally {
      isRequestInProgress.current = false;
    }
  };

  const importFile = async (validation: TreeImportValidation) => {
    if (!selectedFile || isRequestInProgress.current) return;
    isRequestInProgress.current = true;
    setImportState({ status: 'importing', validation });
    try {
      setImportState({ status: 'imported', result: await dataExchangeApi.importTrees(projectId, selectedFile) });
    } catch (importError) {
      const isRejectedFile = importError instanceof ApiRequestError && importError.httpStatus === 400;
      setImportState({ status: 'failed', errorMessage: isRejectedFile
        ? 'Plik zawiera błędy - nic nie zostało zaimportowane. Sprawdź plik ponownie.'
        : `Nie udało się zaimportować drzew - nic nie zostało zapisane. ${errorMessageOf(importError)}` });
    } finally {
      isRequestInProgress.current = false;
    }
  };

  const validation = importState.status === 'validated' || importState.status === 'importing' ? importState.validation : null;
  const isBusy = importState.status === 'validating' || importState.status === 'importing';
  const allErrors = validation ? [...validation.fileErrors, ...validation.errors] : [];
  const visibleErrors = areAllIssuesVisible ? allErrors : allErrors.slice(0, VISIBLE_ISSUE_COUNT);

  return (
    <section className="data-exchange-card stack" aria-labelledby="trees-import-heading">
      <h3 id="trees-import-heading">Import drzew</h3>
      <p className="muted">
        Plik CSV, XLSX, GeoJSON, Shapefile (spakowany do ZIP), GeoPackage albo rysunek DXF. Arkusz potrzebuje kolumny
        z numerem drzewa („Numer inwentaryzacyjny” albo „Etykieta” - typowy eksport aplikacji terenowej). Współrzędne
        w PL-2000 i PL-1992 są przeliczane do WGS 84. Każdy obiekt tworzy nowe drzewo - istniejące nie są zmieniane.
        Jeżeli plik ma błędy, nic nie zostanie zaimportowane.
      </p>
      <div className="field">
        <label htmlFor="trees-import-file">Plik z drzewami</label>
        <input id="trees-import-file" type="file" accept=".csv,.xlsx,.geojson,.json,.zip,.gpkg,.dxf" onChange={selectFile} disabled={isBusy} />
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
            Zaimportowano drzewa: {importState.result.importedTreeCount}.{' '}
            <Link to={`/projects/${projectId}/trees`}>Przejdź do listy drzew</Link>
          </Alert>
        )}
        {validation && (
          <div className="stack data-exchange-validation">
            <ul className="summary-statistics">
              <li className="summary-statistic"><span className="summary-statistic-value">{validation.totalRowCount}</span><span className="summary-statistic-label">Znalezione wiersze</span></li>
              <li className="summary-statistic"><span className="summary-statistic-value">{validation.validRowCount}</span><span className="summary-statistic-label">Gotowe do importu</span></li>
              <li className="summary-statistic"><span className="summary-statistic-value">{validation.invalidRowCount + validation.fileErrors.length}</span><span className="summary-statistic-label">Błędy</span></li>
            </ul>
            {validation.readyToImport
              ? <Alert variant="success">Plik jest poprawny. Można zaimportować drzewa: {validation.validRowCount}.</Alert>
              : <Alert variant="error">Plik zawiera błędy. Popraw je w pliku i sprawdź go ponownie - import jest możliwy tylko bez błędów.</Alert>}
            {allErrors.length > 0 && (
              <div className="stack-tight">
                <h4>Błędy</h4>
                <ul className="data-exchange-issues">
                  {visibleErrors.map((issue, index) => <li key={index}>{describeImportIssue(issue, validation.format)}</li>)}
                </ul>
                {allErrors.length > VISIBLE_ISSUE_COUNT && !areAllIssuesVisible && (
                  <button type="button" className="btn btn-ghost btn-small" onClick={() => setAreAllIssuesVisible(true)}>
                    Pokaż wszystkie błędy ({allErrors.length})
                  </button>
                )}
              </div>
            )}
            {validation.warnings.length > 0 && (
              <div className="stack-tight">
                <h4>Ostrzeżenia</h4>
                <ul className="data-exchange-issues data-exchange-warnings">
                  {validation.warnings.slice(0, VISIBLE_ISSUE_COUNT).map((issue, index) => <li key={index}>{describeImportIssue(issue, validation.format)}</li>)}
                </ul>
              </div>
            )}
            {validation.ignoredColumns.length > 0 && (
              <p className="muted small">Pominięte kolumny: {validation.ignoredColumns.join(', ')}.</p>
            )}
            {validation.preview.length > 0 && (
              <div className="table-wrapper">
                <table>
                  <caption className="muted small">Podgląd pierwszych poprawnych wierszy</caption>
                  <thead><tr><th>Wiersz</th><th>Numer</th><th>Gatunek</th><th>Obwód [cm]</th><th>Wysokość [m]</th><th>Lokalizacja</th></tr></thead>
                  <tbody>
                    {validation.preview.map((row) => (
                      <tr key={row.rowNumber}>
                        <td className="num">{row.rowNumber}</td>
                        <td>{row.inventoryNumber}</td>
                        <td>{row.speciesPolish ?? row.speciesLatin ?? '—'}</td>
                        <td className="num">{formatNumber(row.trunkCircumferenceCm)}</td>
                        <td className="num">{formatNumber(row.heightM)}</td>
                        <td>{row.hasLocation ? 'tak' : 'brak'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {validation.readyToImport && (
              <div className="form-actions data-exchange-actions">
                <button type="button" className="btn btn-primary" onClick={() => importFile(validation)} disabled={isBusy}>
                  {importState.status === 'importing' ? 'Importowanie…' : `Importuj drzewa (${validation.validRowCount})`}
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}

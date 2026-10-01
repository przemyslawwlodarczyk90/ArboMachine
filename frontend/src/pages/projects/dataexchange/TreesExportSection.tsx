import { useEffect, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import type { ProjectSummary } from '../summary/projectSummaryDataTypes';
import { projectSummaryApi } from '../summary/projectSummaryApi';
import { dataExchangeApi, treesExportFileName } from './dataExchangeApi';
import type { TreeExportFormat } from './dataExchangeDataTypes';

const TABULAR_EXPORTS: { format: TreeExportFormat; label: string }[] = [
  { format: 'csv', label: 'Pobierz CSV' },
  { format: 'xlsx', label: 'Pobierz XLSX' },
];
const GIS_EXPORTS: { format: TreeExportFormat; label: string }[] = [
  { format: 'geojson', label: 'Pobierz GeoJSON' },
  { format: 'gpkg', label: 'Pobierz GeoPackage' },
];

/** Plik z bloba - pobranie przez tymczasowy link (token JWT nie trafia do adresu URL). */
function saveBlobAsFile(blob: Blob, fileName: string) {
  const objectUrl = URL.createObjectURL(blob);
  const downloadLink = document.createElement('a');
  downloadLink.href = objectUrl;
  downloadLink.download = fileName;
  downloadLink.click();
  setTimeout(() => URL.revokeObjectURL(objectUrl), 10_000);
}

/**
 * Eksport danych drzew: CSV i XLSX (tabela, wszystkie drzewa) oraz QGIS - GeoJSON i GeoPackage (tylko drzewa
 * z lokalizacja; liczba pominietych z podsumowania projektu jest widoczna przed pobraniem).
 */
export function TreesExportSection({ projectId }: { projectId: number }) {
  const [summary, setSummary] = useState<ProjectSummary | null>(null);
  const [downloadingFormat, setDownloadingFormat] = useState<TreeExportFormat | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isResponseStillWanted = true;
    projectSummaryApi.getProjectSummary(projectId)
      .then((loadedSummary) => { if (isResponseStillWanted) setSummary(loadedSummary); })
      .catch(() => { /* liczby sa tylko informacja - eksport dziala bez nich */ });
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  const download = async (format: TreeExportFormat) => {
    if (downloadingFormat) return;
    setDownloadingFormat(format);
    setErrorMessage(null);
    try {
      saveBlobAsFile(await dataExchangeApi.downloadTreesExport(projectId, format), treesExportFileName(projectId, format));
    } catch (downloadError) {
      setErrorMessage(`Nie udało się pobrać pliku. ${downloadError instanceof ApiRequestError ? downloadError.message : 'Nieoczekiwany błąd.'}`);
    } finally {
      setDownloadingFormat(null);
    }
  };

  const exportButton = ({ format, label }: { format: TreeExportFormat; label: string }) => (
    <button key={format} type="button" className="btn btn-secondary" onClick={() => download(format)}
            disabled={downloadingFormat !== null} aria-busy={downloadingFormat === format}>
      {downloadingFormat === format ? 'Pobieranie…' : label}
    </button>
  );

  return (
    <section className="data-exchange-card stack" aria-labelledby="trees-export-heading">
      <h3 id="trees-export-heading">Eksport danych</h3>
      <p className="muted">
        Tabela wszystkich drzew projektu do otwarcia w Excelu albo LibreOffice. CSV: separator „;”, kodowanie UTF-8,
        przecinek dziesiętny. XLSX: arkusze „Drzewa” i „Projekt”.
      </p>
      <div className="form-actions data-exchange-actions">{TABULAR_EXPORTS.map(exportButton)}</div>

      <div className="data-exchange-gis stack">
        <h4>QGIS</h4>
        <p className="muted">Dane przestrzenne projektu (punkty drzew z atrybutami) można otworzyć w QGIS.</p>
        <dl className="data-exchange-facts">
          <dt>Formaty</dt><dd>GeoJSON, GeoPackage (warstwa „trees”)</dd>
          <dt>Układ współrzędnych</dt><dd>WGS 84 (EPSG:4326)</dd>
          <dt>Geometria</dt><dd>punkt (Point) - jedno drzewo z lokalizacją</dd>
        </dl>
        {summary && summary.treeWithoutLocationCount > 0 && (
          <Alert variant="info">
            Drzewa bez lokalizacji ({summary.treeWithoutLocationCount} z {summary.treeCount}) nie trafią do plików GeoJSON
            i GeoPackage - są tylko w CSV i XLSX.
          </Alert>
        )}
        <div className="form-actions data-exchange-actions">{GIS_EXPORTS.map(exportButton)}</div>
        <details className="data-exchange-help">
          <summary>Jak użyć w QGIS</summary>
          <ol>
            <li>Pobierz plik GeoPackage albo GeoJSON.</li>
            <li>W QGIS wybierz <strong>Warstwa → Dodaj warstwę → Dodaj warstwę wektorową</strong> (albo przeciągnij plik na mapę).</li>
            <li>Wskaż pobrany plik. Dla GeoPackage wybierz warstwę <strong>trees</strong>.</li>
            <li>Układ współrzędnych warstwy to EPSG:4326 - QGIS przeliczy go na układ projektu mapy (np. PL-1992).</li>
            <li>Atrybuty drzew zobaczysz w <strong>tabeli atrybutów</strong> (prawy przycisk na warstwie).</li>
            <li>Aby wrócić z danymi do ArboMachine, zapisz warstwę jako GeoJSON w EPSG:4326 i zaimportuj ją niżej -
              import <strong>tworzy nowe drzewa</strong>, nie aktualizuje istniejących.</li>
          </ol>
        </details>
      </div>
      {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
    </section>
  );
}

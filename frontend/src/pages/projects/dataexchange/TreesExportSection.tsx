import { useEffect, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { ToBeClarifiedNote } from '../../../components/ToBeClarifiedNote';
import type { ProjectSummary } from '../summary/projectSummaryDataTypes';
import { projectSummaryApi } from '../summary/projectSummaryApi';
import { dataExchangeApi, projectExportFileName } from './dataExchangeApi';
import { saveBlobAsFile } from './saveBlobAsFile';
import type { ProjectExportFile } from './dataExchangeDataTypes';
import { DEFAULT_GIS_EXPORT_CRS, GIS_EXPORT_CRS_OPTIONS, type GisExportCrs } from './gisExportCrs';

const TABULAR_EXPORTS: { format: ProjectExportFile; label: string }[] = [
  { format: 'csv', label: 'Pobierz CSV' },
  { format: 'xlsx', label: 'Pobierz XLSX' },
  { format: 'works-csv', label: 'Pobierz prace (CSV)' },
  { format: 'calendar-ics', label: 'Pobierz kalendarz prac (ICS)' },
];
/** Etap 34: w wybranym ukladzie wspolrzednych, ze stylami QGIS i warstwa oznaczen. */
const QGIS_EXPORTS: { format: ProjectExportFile; label: string }[] = [
  { format: 'qgis-zip', label: 'Pobierz pakiet dla QGIS (ZIP)' },
  { format: 'gpkg', label: 'Pobierz GeoPackage' },
];
/** Zawsze WGS 84 (EPSG:4326). */
const GIS_EXPORTS: { format: ProjectExportFile; label: string }[] = [
  { format: 'geojson', label: 'Pobierz GeoJSON' },
  { format: 'shapefile-zip', label: 'Pobierz Shapefile (ZIP)' },
  { format: 'stands-geojson', label: 'Pobierz grupy drzew (GeoJSON)' },
];

/**
 * Eksport danych drzew: CSV i XLSX (tabela, wszystkie drzewa) oraz QGIS - GeoJSON i GeoPackage (tylko drzewa
 * z lokalizacja; liczba pominietych z podsumowania projektu jest widoczna przed pobraniem).
 */
export function TreesExportSection({ projectId }: { projectId: number }) {
  const [summary, setSummary] = useState<ProjectSummary | null>(null);
  const [downloadingFormat, setDownloadingFormat] = useState<ProjectExportFile | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [exportCrs, setExportCrs] = useState<GisExportCrs>(DEFAULT_GIS_EXPORT_CRS);

  useEffect(() => {
    let isResponseStillWanted = true;
    projectSummaryApi.getProjectSummary(projectId)
      .then((loadedSummary) => { if (isResponseStillWanted) setSummary(loadedSummary); })
      .catch(() => { /* liczby sa tylko informacja - eksport dziala bez nich */ });
    return () => { isResponseStillWanted = false; };
  }, [projectId]);

  const download = async (format: ProjectExportFile) => {
    if (downloadingFormat) return;
    setDownloadingFormat(format);
    setErrorMessage(null);
    try {
      saveBlobAsFile(await dataExchangeApi.downloadProjectExport(projectId, format, exportCrs), projectExportFileName(projectId, format));
    } catch (downloadError) {
      setErrorMessage(`Nie udało się pobrać pliku. ${downloadError instanceof ApiRequestError ? downloadError.message : 'Nieoczekiwany błąd.'}`);
    } finally {
      setDownloadingFormat(null);
    }
  };

  const exportButton = ({ format, label }: { format: ProjectExportFile; label: string }) => (
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
        przecinek dziesiętny. XLSX: arkusze „Drzewa”, „Prace”, „Monitoring” i „Projekt”. „Pobierz prace (CSV)” - jeden wiersz na
        zalecenie lub pracę (status, priorytet, termin, data wykonania).
      </p>
      <div className="form-actions data-exchange-actions">{TABULAR_EXPORTS.map(exportButton)}</div>

      <div className="data-exchange-gis stack">
        <h4>QGIS</h4>
        <p className="muted">
          Dane przestrzenne projektu do otwarcia w QGIS. Pakiet dla QGIS zawiera GeoPackage z warstwami drzew, oznaczeń
          i grup drzew wraz z gotowymi stylami (kolory i skróty oznaczeń jak w aplikacji, numery drzew).
        </p>
        <dl className="data-exchange-facts">
          <dt>Pakiet dla QGIS, GeoPackage</dt><dd>warstwy „trees”, „tree_markings” (jedno oznaczenie = jeden punkt), „stands”; style zapisane w pliku</dd>
          <dt>Układ współrzędnych</dt><dd>do wyboru niżej (GeoJSON, Shapefile i grupy drzew: zawsze WGS 84)</dd>
          <dt>Geometria</dt><dd>punkt - jedno drzewo z lokalizacją; obszar - grupa drzew</dd>
        </dl>
        <div className="field">
          <label htmlFor="gis-export-crs">Układ współrzędnych pakietu dla QGIS i GeoPackage</label>
          <select id="gis-export-crs" className="select" value={exportCrs}
                  onChange={(changeEvent) => setExportCrs(changeEvent.target.value as GisExportCrs)}>
            {GIS_EXPORT_CRS_OPTIONS.map((option) => <option key={option.crs} value={option.crs}>{option.label}</option>)}
          </select>
        </div>
        <div className="form-actions data-exchange-actions">{QGIS_EXPORTS.map(exportButton)}</div>
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
            <li>Pobierz pakiet dla QGIS i rozpakuj go (instrukcja jest też w pliku README.txt).</li>
            <li>Przeciągnij plik <strong>.gpkg</strong> na mapę QGIS (albo <strong>Warstwa → Dodaj warstwę → Dodaj warstwę
              wektorową</strong>) i zaznacz wszystkie warstwy: trees, tree_markings, stands.</li>
            <li>Style wczytują się same. Jeśli nie: <strong>Właściwości warstwy → Styl → Wczytaj styl</strong> i plik .qml
              o nazwie warstwy z pakietu.</li>
            <li>Układ współrzędnych jest zapisany w pliku - QGIS go rozpozna i w razie potrzeby przeliczy na układ projektu mapy.</li>
            <li>Atrybuty drzew zobaczysz w <strong>tabeli atrybutów</strong> (prawy przycisk na warstwie).</li>
            <li>Aby wrócić z danymi do ArboMachine, zapisz warstwę jako GeoJSON w EPSG:4326 i zaimportuj ją niżej -
              import <strong>tworzy nowe drzewa</strong>, nie aktualizuje istniejących.</li>
          </ol>
          <ToBeClarifiedNote>
            Style to wersja V1, jeszcze nie sprawdzona w samym QGIS. Do ustalenia: wersja QGIS, układ wymagany przez gminę
            i własna symbolika oznaczeń.
          </ToBeClarifiedNote>
        </details>
      </div>
      {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
    </section>
  );
}

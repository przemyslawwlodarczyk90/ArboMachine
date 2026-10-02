import { useParams } from 'react-router-dom';
import { parsePositiveIdRouteParam } from '../../../routing/parsePositiveIdRouteParam';
import { TreesExportSection } from './TreesExportSection';
import { StandsImportSection } from './StandsImportSection';
import { TreesImportSection } from './TreesImportSection';

/**
 * Sekcja "Import i eksport" workspace projektu (/projects/:projectId/import-export): eksport drzew (CSV, XLSX,
 * GeoJSON, GeoPackage dla QGIS) i import drzew z pliku. Renderuje sie po udanym pobraniu projektu (ProjectPage).
 */
export function ProjectDataExchangePage() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  if (projectId === null) return null;
  return (
    <div className="stack">
      <h2>Import i eksport</h2>
      <TreesExportSection projectId={projectId} />
      <TreesImportSection projectId={projectId} />
      <StandsImportSection projectId={projectId} />
    </div>
  );
}

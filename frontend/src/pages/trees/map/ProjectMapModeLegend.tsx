import { Link } from 'react-router-dom';
import type { ProjectMapLegendEntry } from '../../../map/mapSymbolLegend';
import { TreeMarkingBadge } from '../markings/TreeMarkingBadge';
import type { TreeMarkingDefinition } from '../markings/treeMarkingDataTypes';
import { valuationColorOf } from '../valuation/valuationColors';
import { valuationCategoryLabel } from '../valuation/valuationDataTypes';
import { buildManagementLegend, managementColorOf, managementStateLabel } from '../works/workManagement';
import { ProjectMapLegend } from './ProjectMapLegend';
import { buildMarkingLegend, buildValuationLegend, type MapViewMode } from './mapViewMode';
import type { TreeMapPoint } from './treeMapPoints';

/**
 * Legenda biezacego trybu mapy projektu (wydzielona w etapie 32 z mapy biurowej, uzywana takze w arkuszu "Warstwy"
 * trybu terenowego): symbole prac, oznaczenia drzew, waloryzacja albo gospodarka drzewostanem.
 */
export function ProjectMapModeLegend({ viewMode, treeMapPoints, allTreeMapPoints, legendEntries, markingCatalogCodes,
                                      markingByCode, projectId }: {
  viewMode: MapViewMode;
  /** punkty widoczne na mapie (po filtrze oznaczen) */
  treeMapPoints: TreeMapPoint[];
  allTreeMapPoints: TreeMapPoint[];
  legendEntries: ProjectMapLegendEntry[];
  markingCatalogCodes: string[];
  markingByCode: Map<string, TreeMarkingDefinition>;
  projectId: number;
}) {
  return (
    <>
      {treeMapPoints.length > 0 && viewMode === 'SYMBOLS' && <ProjectMapLegend entries={legendEntries} />}
      {allTreeMapPoints.length > 0 && viewMode === 'MARKINGS' && (
        <section className="map-legend" aria-labelledby="map-marking-legend-heading">
          <h3 id="map-marking-legend-heading">Legenda - oznaczenia drzew</h3>
          {buildMarkingLegend(allTreeMapPoints, markingCatalogCodes).length === 0 ? (
            <p className="muted small">Żadne drzewo na mapie nie ma jeszcze oznaczeń. Oznaczenia dodasz w karcie drzewa.</p>
          ) : (
            <ul>
              {buildMarkingLegend(allTreeMapPoints, markingCatalogCodes).map(({ code, count }) => (
                <li key={code}><TreeMarkingBadge marking={markingByCode.get(code)!} withName /> ({count})</li>
              ))}
            </ul>
          )}
          <p className="muted small">Przy drzewie widać do 4 oznaczeń, resztę jako „+N” - pełna lista po kliknięciu drzewa.</p>
        </section>
      )}
      {treeMapPoints.length > 0 && viewMode === 'VALUATION' && (
        <section className="map-legend" aria-labelledby="map-valuation-legend-heading">
          <h3 id="map-valuation-legend-heading">Legenda - waloryzacja</h3>
          <ul>
            {buildValuationLegend(treeMapPoints).map(({ category, count }) => (
              <li key={category ?? 'NONE'}>
                <span className="map-legend-swatch-circle" style={{ background: valuationColorOf(category).fill }} aria-hidden="true" />
                {' '}{valuationCategoryLabel(category)} ({count})
              </li>
            ))}
          </ul>
          <p className="muted small">
            Kategorie i kolory są tymczasowe (wersja V1) - to nie jest skala branżowa. Ocenę ogólną wybiera arborysta w karcie drzewa.
          </p>
        </section>
      )}
      {treeMapPoints.length > 0 && viewMode === 'MANAGEMENT' && (
        <section className="map-legend" aria-labelledby="map-management-legend-heading">
          <h3 id="map-management-legend-heading">Legenda - gospodarka drzewostanem</h3>
          <ul>
            {buildManagementLegend(treeMapPoints).map(({ state, count }) => (
              <li key={state}>
                <span className="map-legend-swatch-circle" style={{ background: managementColorOf(state) }} aria-hidden="true" />
                {' '}{managementStateLabel(state)} ({count})
              </li>
            ))}
          </ul>
          <p className="muted small">
            Prace do wykonania = zalecenia i prace zaplanowane. Przy drzewie widać symbole tylko tych prac. Pełna lista:{' '}
            <Link to={`/projects/${projectId}/management`}>Gospodarka drzewostanem</Link>.
          </p>
        </section>
      )}
    </>
  );
}

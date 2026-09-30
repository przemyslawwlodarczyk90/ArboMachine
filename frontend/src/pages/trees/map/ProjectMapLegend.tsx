import { MapSymbolSwatch } from '../../../map/MapSymbolSwatch';
import type { ProjectMapLegendEntry } from '../../../map/mapSymbolLegend';

/** Legenda mapy projektu - wyliczana z danych (buildProjectMapLegend), bez recznej edycji i bez zapisu w bazie. */
export function ProjectMapLegend({ entries }: { entries: ProjectMapLegendEntry[] }) {
  return (
    <section className="map-legend" aria-labelledby="map-legend-heading">
      <h3 id="map-legend-heading">Legenda</h3>
      <ul>
        <li><span className="map-legend-neutral" aria-hidden="true" /> Drzewo bez symbolu</li>
        {entries.map((entry) => (
          <li key={`${entry.kind}-${entry.symbol.code}-${entry.label}`}>
            <MapSymbolSwatch symbol={entry.symbol} size={20} /> {entry.label}
          </li>
        ))}
      </ul>
      {entries.length === 0 && (
        <p className="muted small">Symbole przypiszesz w karcie drzewa: symbol drzewa i symbole zaleceń/prac.</p>
      )}
    </section>
  );
}

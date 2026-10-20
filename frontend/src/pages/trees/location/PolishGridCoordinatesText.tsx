import {
  describeZone, formatGridMeters, PL_1992_ZONE, pl2000ZoneForLongitude, toPolishGrid,
} from '../../../map/polishGridCoordinates';
import type { TreeLocation } from '../treeDataTypes';

/**
 * Zapisany punkt drzewa w ukladach krajowych (etap 31): PL-2000 w strefie wynikajacej z dlugosci i PL-1992.
 * Tylko wyswietlanie - backend przechowuje WGS 84; X = polnoc, Y = wschod (konwencja geodezyjna).
 */
export function PolishGridCoordinatesText({ location }: { location: TreeLocation }) {
  const zones = [pl2000ZoneForLongitude(location.longitude), PL_1992_ZONE];
  return (
    <span className="tree-location-grid muted small">
      {zones.map((zone) => {
        const { x, y } = toPolishGrid(location.latitude, location.longitude, zone);
        return (
          <span key={zone.epsgCode}>
            {describeZone(zone)}: X = {formatGridMeters(x)} · Y = {formatGridMeters(y)}
          </span>
        );
      })}
    </span>
  );
}

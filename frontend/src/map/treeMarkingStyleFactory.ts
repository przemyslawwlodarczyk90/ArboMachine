import { Circle as CircleStyle, Fill, Stroke, Style, Text } from 'ol/style';
import { createOverflowBadgeStyle } from './mapSymbolStyleFactory';
import { layoutMarkingBadges } from './treeMarkingLayout';

/** To, co mapa musi wiedziec o oznaczeniu (katalog GET /api/tree-markings). */
export interface MapMarkingAppearance {
  shortLabel: string;
  color: string;
}

const MARKING_BADGE_RADIUS_PX = 8;

/** Kolko w kolorze oznaczenia z bialym skrotem, przesuniete o displacement (y w gore); Text liczy offset y w dol. */
function markingBadgeStyles(marking: MapMarkingAppearance, displacement: [number, number]): Style[] {
  return [
    new Style({ image: new CircleStyle({
      radius: MARKING_BADGE_RADIUS_PX, displacement,
      fill: new Fill({ color: marking.color }),
      stroke: new Stroke({ color: '#ffffff', width: 1.5 }),
    }) }),
    new Style({ text: new Text({
      text: marking.shortLabel,
      offsetX: displacement[0], offsetY: -displacement[1],
      font: `bold ${marking.shortLabel.length > 1 ? 8 : 10}px system-ui, sans-serif`,
      fill: new Fill({ color: '#ffffff' }),
    }) }),
  ];
}

/** Oznaczenia drzewa nad punktem (rzad + "+N"); nieznany kod (spoza katalogu) pominiety zamiast bledu mapy. */
export function createTreeMarkingStyles(markingCodes: string[], markingByCode: Map<string, MapMarkingAppearance>): Style[] {
  const layout = layoutMarkingBadges(markingCodes.filter((code) => markingByCode.has(code)));
  const styles = layout.badges.flatMap((badge) => markingBadgeStyles(markingByCode.get(badge.code)!, badge.displacement));
  if (layout.overflowCount > 0) styles.push(createOverflowBadgeStyle(layout.overflowCount, layout.overflowDisplacement));
  return styles;
}

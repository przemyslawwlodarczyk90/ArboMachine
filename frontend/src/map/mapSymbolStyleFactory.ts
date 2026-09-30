import { Circle as CircleStyle, Fill, RegularShape, Stroke, Style, Text } from 'ol/style';
import type { MapSymbol } from './mapSymbolDataTypes';
import { toMapSymbolShapeSpec } from './mapSymbolShapeSpec';

// Symbol z katalogu -> styl OpenLayers (V1: CircleStyle/RegularShape). Jedyne miejsce, ktore wie, jak symbol wyglada
// na mapie - pozniej mozna tu podmienic ksztalty na ol/style/Icon z plikami SVG bez zmian w danych (mapSymbolCode).

export interface MapSymbolStyleOptions {
  /** promien symbolu w pikselach */
  radius: number;
  /** przesuniecie w pikselach wzgledem punktu (znaczki prac wokol drzewa); wspolrzedne punktu sie nie zmieniaja */
  displacement?: [number, number];
}

export function createMapSymbolStyles(symbol: MapSymbol, { radius, displacement = [0, 0] }: MapSymbolStyleOptions): Style[] {
  const shapeSpec = toMapSymbolShapeSpec(symbol.shape);
  if (shapeSpec.kind === 'circle') {
    return [new Style({ image: new CircleStyle({
      radius, displacement,
      fill: new Fill({ color: symbol.fillColor }),
      stroke: new Stroke({ color: symbol.strokeColor, width: 2 }),
    }) })];
  }
  if (shapeSpec.isStrokeOnly) {
    // Krzyz / iks z samych linii: jasna obwodka pod spodem, zeby byl widoczny na kazdym podkladzie.
    const lineShape = (color: string, width: number) => new RegularShape({
      points: shapeSpec.points, radius: radius + 1, radius2: shapeSpec.radius2 ?? 0, angle: shapeSpec.angle, displacement,
      stroke: new Stroke({ color, width }),
    });
    return [new Style({ image: lineShape(symbol.strokeColor, 6) }), new Style({ image: lineShape(symbol.fillColor, 3) })];
  }
  return [new Style({ image: new RegularShape({
    points: shapeSpec.points, radius: shapeSpec.points === 3 ? radius + 2 : radius + 1, angle: shapeSpec.angle, displacement,
    fill: new Fill({ color: symbol.fillColor }),
    stroke: new Stroke({ color: symbol.strokeColor, width: 2 }),
  }) })];
}

/** Znacznik "+N" przy drzewie, gdy prac z symbolami jest wiecej niz miesci sie wokol punktu. */
export function createOverflowBadgeStyle(overflowCount: number, displacement: [number, number]): Style {
  return new Style({ text: new Text({
    text: `+${overflowCount}`,
    // Text uzywa offsetu ekranowego (y w dol), a displacement - y w gore.
    offsetX: displacement[0], offsetY: -displacement[1],
    font: 'bold 12px system-ui, sans-serif',
    fill: new Fill({ color: '#1d2419' }),
    stroke: new Stroke({ color: '#ffffff', width: 3 }),
  }) });
}

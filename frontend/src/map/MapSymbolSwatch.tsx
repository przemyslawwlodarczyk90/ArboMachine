import type { MapSymbol } from './mapSymbolDataTypes';
import { toMapSymbolShapeSpec } from './mapSymbolShapeSpec';

/** Wierzcholki wielokata foremnego w ukladzie SVG (y w dol) - te same parametry co RegularShape na mapie. */
function polygonPoints(points: number, radius: number, angle: number, center: number): string {
  return Array.from({ length: points }, (_, index) => {
    const pointAngle = angle + (2 * Math.PI * index) / points;
    return `${(center + radius * Math.sin(pointAngle)).toFixed(2)},${(center - radius * Math.cos(pointAngle)).toFixed(2)}`;
  }).join(' ');
}

/**
 * Miniatura symbolu (SVG) do wyboru symbolu, legendy i list prac - wyglada jak symbol na mapie, bo korzysta
 * z tego samego opisu ksztaltu. Zawsze ksztalt + kolor (samo rozroznienie kolorem nie wystarcza).
 */
export function MapSymbolSwatch({ symbol, size = 22 }: { symbol: MapSymbol; size?: number }) {
  const shapeSpec = toMapSymbolShapeSpec(symbol.shape);
  const center = size / 2;
  const radius = size / 2 - 3;
  let shapeElement;
  if (shapeSpec.kind === 'circle') {
    shapeElement = <circle cx={center} cy={center} r={radius} fill={symbol.fillColor} stroke={symbol.strokeColor} strokeWidth={2} />;
  } else if (shapeSpec.isStrokeOnly) {
    // Krzyz (0 rad) albo iks (45 stopni): dwie linie przez srodek, jasna obwodka pod spodem jak na mapie.
    const lines = [0, Math.PI / 2].map((extraAngle) => {
      const lineAngle = shapeSpec.angle + extraAngle;
      const deltaX = radius * Math.sin(lineAngle);
      const deltaY = radius * Math.cos(lineAngle);
      return [center - deltaX, center + deltaY, center + deltaX, center - deltaY];
    });
    shapeElement = (
      <g strokeLinecap="round">
        {lines.map(([x1, y1, x2, y2], index) => (
          <line key={`outline-${index}`} x1={x1} y1={y1} x2={x2} y2={y2} stroke={symbol.strokeColor} strokeWidth={5} />))}
        {lines.map(([x1, y1, x2, y2], index) => (
          <line key={`line-${index}`} x1={x1} y1={y1} x2={x2} y2={y2} stroke={symbol.fillColor} strokeWidth={3} />))}
      </g>
    );
  } else {
    shapeElement = <polygon points={polygonPoints(shapeSpec.points, radius, shapeSpec.angle, center)}
                            fill={symbol.fillColor} stroke={symbol.strokeColor} strokeWidth={2} />;
  }
  return (
    <svg className="map-symbol-swatch" width={size} height={size} viewBox={`0 0 ${size} ${size}`} aria-hidden="true">
      {shapeElement}
    </svg>
  );
}

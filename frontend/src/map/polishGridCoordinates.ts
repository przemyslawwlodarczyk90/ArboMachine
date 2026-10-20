// Polskie uklady plaskie (etap 31): PL-2000 (strefy 5-8, EPSG:2176-2179) i PL-1992 (EPSG:2180) <-> WGS 84.
// Odwzorowanie Gaussa-Krugera na elipsoidzie GRS80 szeregami Krugera (n^4) - ta sama matematyka co backend
// (PolishGridCrs), dokladnosc ponizej milimetra w zasiegu Polski. Bez bibliotek; modul testowany przez node --test.
// Konwencja geodezyjna: X = northing (polnoc), Y = easting (wschod), w metrach.

export type PolishGridSystem = 'PL_2000' | 'PL_1992';

export interface PolishGridZone {
  system: PolishGridSystem;
  epsgCode: number;
  /** strefa PL-2000 (5-8); null dla PL-1992 */
  zoneNumber: number | null;
  centralMeridianDegrees: number;
  scaleFactor: number;
  falseEasting: number;
  falseNorthing: number;
}

export const PL_1992_ZONE: PolishGridZone = {
  system: 'PL_1992', epsgCode: 2180, zoneNumber: null, centralMeridianDegrees: 19, scaleFactor: 0.9993,
  falseEasting: 500_000, falseNorthing: -5_300_000,
};

export const PL_2000_ZONES: PolishGridZone[] = [5, 6, 7, 8].map((zoneNumber) => ({
  system: 'PL_2000', epsgCode: 2171 + zoneNumber, zoneNumber, centralMeridianDegrees: zoneNumber * 3,
  scaleFactor: 0.999923, falseEasting: zoneNumber * 1_000_000 + 500_000, falseNorthing: 0,
}));

/** Strefa PL-2000 dla dlugosci geograficznej: poludnik osiowy 15/18/21/24 +- 1,5 stopnia (poza Polska - najblizsza). */
export function pl2000ZoneForLongitude(longitude: number): PolishGridZone {
  const zoneNumber = Math.min(8, Math.max(5, Math.round(longitude / 3)));
  return PL_2000_ZONES.find((zone) => zone.zoneNumber === zoneNumber)!;
}

export function pl2000ZoneByNumber(zoneNumber: number): PolishGridZone | null {
  return PL_2000_ZONES.find((zone) => zone.zoneNumber === zoneNumber) ?? null;
}

const SEMI_MAJOR_AXIS = 6_378_137.0;
const FLATTENING = 1 / 298.257222101;
const N = FLATTENING / (2 - FLATTENING);
const RECTIFYING_RADIUS = SEMI_MAJOR_AXIS / (1 + N) * (1 + N ** 2 / 4 + N ** 4 / 64);
const ALPHA = [
  N / 2 - 2 * N ** 2 / 3 + 5 * N ** 3 / 16 + 41 * N ** 4 / 180,
  13 * N ** 2 / 48 - 3 * N ** 3 / 5 + 557 * N ** 4 / 1440,
  61 * N ** 3 / 240 - 103 * N ** 4 / 140,
  49561 * N ** 4 / 161280,
];
const BETA = [
  N / 2 - 2 * N ** 2 / 3 + 37 * N ** 3 / 96 - N ** 4 / 360,
  N ** 2 / 48 + N ** 3 / 15 - 437 * N ** 4 / 1440,
  17 * N ** 3 / 480 - 37 * N ** 4 / 840,
  4397 * N ** 4 / 161280,
];
const DELTA = [
  2 * N - 2 * N ** 2 / 3 - 2 * N ** 3 + 116 * N ** 4 / 45,
  7 * N ** 2 / 3 - 8 * N ** 3 / 5 - 227 * N ** 4 / 45,
  56 * N ** 3 / 15 - 136 * N ** 4 / 35,
  4279 * N ** 4 / 630,
];
const ECCENTRICITY = Math.sqrt(FLATTENING * (2 - FLATTENING));
const toRadians = (degrees: number) => degrees * Math.PI / 180;
const toDegrees = (radians: number) => radians * 180 / Math.PI;

export interface PlanarCoordinates {
  /** northing (m) */
  x: number;
  /** easting (m) */
  y: number;
}

/** WGS 84 (stopnie) -> wspolrzedne plaskie strefy. */
export function toPolishGrid(latitude: number, longitude: number, zone: PolishGridZone): PlanarCoordinates {
  const phi = toRadians(latitude);
  const lambda = toRadians(longitude - zone.centralMeridianDegrees);
  const t = Math.sinh(Math.atanh(Math.sin(phi)) - ECCENTRICITY * Math.atanh(ECCENTRICITY * Math.sin(phi)));
  const xiPrime = Math.atan2(t, Math.cos(lambda));
  const etaPrime = Math.atanh(Math.sin(lambda) / Math.sqrt(1 + t * t));
  let xi = xiPrime;
  let eta = etaPrime;
  for (let j = 1; j <= 4; j++) {
    xi += ALPHA[j - 1] * Math.sin(2 * j * xiPrime) * Math.cosh(2 * j * etaPrime);
    eta += ALPHA[j - 1] * Math.cos(2 * j * xiPrime) * Math.sinh(2 * j * etaPrime);
  }
  return {
    x: zone.scaleFactor * RECTIFYING_RADIUS * xi + zone.falseNorthing,
    y: zone.scaleFactor * RECTIFYING_RADIUS * eta + zone.falseEasting,
  };
}

/** Wspolrzedne plaskie strefy -> WGS 84 (stopnie). */
export function fromPolishGrid(coordinates: PlanarCoordinates, zone: PolishGridZone): { latitude: number; longitude: number } {
  const xi = (coordinates.x - zone.falseNorthing) / (zone.scaleFactor * RECTIFYING_RADIUS);
  const eta = (coordinates.y - zone.falseEasting) / (zone.scaleFactor * RECTIFYING_RADIUS);
  let xiPrime = xi;
  let etaPrime = eta;
  for (let j = 1; j <= 4; j++) {
    xiPrime -= BETA[j - 1] * Math.sin(2 * j * xi) * Math.cosh(2 * j * eta);
    etaPrime -= BETA[j - 1] * Math.cos(2 * j * xi) * Math.sinh(2 * j * eta);
  }
  const chi = Math.asin(Math.sin(xiPrime) / Math.cosh(etaPrime));
  let latitude = chi;
  for (let j = 1; j <= 4; j++) latitude += DELTA[j - 1] * Math.sin(2 * j * chi);
  return {
    latitude: toDegrees(latitude),
    longitude: zone.centralMeridianDegrees + toDegrees(Math.atan2(Math.sinh(etaPrime), Math.cos(xiPrime))),
  };
}

/** Wspolrzedna w metrach po polsku: odstep tysiecy, przecinek, 2 miejsca (cm), np. "5 788 456,49". */
export function formatGridMeters(value: number): string {
  const [integerPart, fractionPart] = value.toFixed(2).split('.');
  return `${integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')},${fractionPart}`;
}

/** Opis strefy do UI, np. "PL-2000 strefa 7 (EPSG:2178)" albo "PL-1992 (EPSG:2180)". */
export function describeZone(zone: PolishGridZone): string {
  return zone.system === 'PL_2000'
    ? `PL-2000 strefa ${zone.zoneNumber} (EPSG:${zone.epsgCode})`
    : `PL-1992 (EPSG:${zone.epsgCode})`;
}

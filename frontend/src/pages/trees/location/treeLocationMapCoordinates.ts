import type { Coordinate } from 'ol/coordinate.js';
// Importy OpenLayers z rozszerzeniem .js: modul jest testowany bezposrednio w Node (node --test),
// a pakiet ol nie ma mapy "exports" pozwalajacej pominac rozszerzenie. Vite i TypeScript rozumieja obie formy.
import { fromLonLat, toLonLat } from 'ol/proj.js';
import type { TreeLocation } from '../treeDataTypes';

/**
 * Jedyne miejsce zamiany TreeLocation <-> wspolrzedne mapy. Uwaga na kolejnosc:
 * OpenLayers uzywa [longitude, latitude] (x, y), a API { latitude, longitude }.
 * Mapa renderuje w EPSG:3857 (domyslna projekcja widoku OpenLayers); TreeLocation i baza zostaja w WGS84 -
 * wspolrzednych projekcji mapy nigdy nie wysylamy do backendu.
 */

/** 7 miejsc po przecinku (~1 cm) - skala kolumn backendu; backend i tak normalizuje i zwraca zapisany punkt. */
const TREE_LOCATION_DECIMAL_PLACES = 7;

const roundToTreeLocationScale = (degrees: number) => Number(degrees.toFixed(TREE_LOCATION_DECIMAL_PLACES));

/** TreeLocation (WGS84) -> wspolrzedna mapy (EPSG:3857). */
export const toMapCoordinate = ({ latitude, longitude }: TreeLocation): Coordinate => fromLonLat([longitude, latitude]);

/**
 * Wspolrzedna klikniecia na mapie (EPSG:3857) -> TreeLocation (WGS84). toLonLat sprowadza dlugosc do -180..180,
 * gdy uzytkownik przewinal mape "dookola swiata".
 */
export const toTreeLocation = (mapCoordinate: Coordinate): TreeLocation => {
  const [longitude, latitude] = toLonLat(mapCoordinate);
  return { latitude: roundToTreeLocationScale(latitude), longitude: roundToTreeLocationScale(longitude) };
};

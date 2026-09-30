import 'ol/ol.css';
import Map from 'ol/Map';
import View from 'ol/View';
import { boundingExtent } from 'ol/extent';
import type { Coordinate } from 'ol/coordinate';
import type BaseLayer from 'ol/layer/Base';
import TileLayer from 'ol/layer/Tile';
import { fromLonLat } from 'ol/proj';
import OSM from 'ol/source/OSM';
import { Circle as CircleStyle, Fill, Stroke, Style } from 'ol/style';

// Wspolna, techniczna konfiguracja map OpenLayers (mapa projektu i mapa lokalizacji drzewa). Tylko to, co obie
// mapy naprawde robia tak samo - bez "frameworka mapowego". Podklad V1 to OpenStreetMap; WMS/Geoportal/ortofoto
// i uklady PL-2000/PL-1992 swiadomie poza zakresem (docs/DO_DOPRECYZOWANIA.md).

/** Widok startowy, gdy nie ma punktu do pokazania: cala Polska. Nie zgadujemy polozenia uzytkownika. */
const DEFAULT_VIEW_CENTER_LONGITUDE_LATITUDE = [19.4, 52.1];
const DEFAULT_VIEW_ZOOM = 6;

/** Przyblizenie przy pokazaniu drzewa - poziom ulicy/dzialki, a nie ekstremalny zoom na pojedynczy punkt. */
export const TREE_FOCUS_ZOOM = 18;
/** Margines (px) przy dopasowaniu widoku do markerow, zeby punkty nie lezaly na krawedzi mapy. */
const FIT_VIEW_PADDING_PX = 48;

/** Mapa z podkladem OSM i warstwami nad nim, osadzona w podanym elemencie. Widok w EPSG:3857 (domyslny OpenLayers). */
export function createMapWithOpenStreetMapBase(targetElement: HTMLElement, overlayLayers: BaseLayer[]): Map {
  return new Map({
    target: targetElement,
    layers: [new TileLayer({ source: new OSM() }), ...overlayLayers],
    view: new View({
      center: fromLonLat(DEFAULT_VIEW_CENTER_LONGITUDE_LATITUDE),
      zoom: DEFAULT_VIEW_ZOOM,
      // OSM nie ma kafelkow powyzej 19 - glebszy zoom pokazywalby rozmyty podklad.
      maxZoom: 19,
    }),
  });
}

export function showDefaultView(map: Map) {
  map.getView().setCenter(fromLonLat(DEFAULT_VIEW_CENTER_LONGITUDE_LATITUDE));
  map.getView().setZoom(DEFAULT_VIEW_ZOOM);
}

/**
 * Dopasowuje widok do punktow (wspolrzedne mapy). Jeden punkt (zerowy zasieg) konczy sie na TREE_FOCUS_ZOOM
 * dzieki maxZoom; brak punktow -> widok domyslny.
 */
export function fitViewToCoordinates(map: Map, mapCoordinates: Coordinate[]) {
  if (mapCoordinates.length === 0) {
    showDefaultView(map);
    return;
  }
  map.getView().fit(boundingExtent(mapCoordinates), {
    // Rozmiar podajemy jawnie - zaraz po utworzeniu mapy widok moze jeszcze nie znac wymiarow kontenera.
    size: map.getSize(),
    padding: [FIT_VIEW_PADDING_PX, FIT_VIEW_PADDING_PX, FIT_VIEW_PADDING_PX, FIT_VIEW_PADDING_PX],
    maxZoom: TREE_FOCUS_ZOOM,
  });
}

/** Neutralny marker drzewa (kolko). Kolorowanie wg stanu/prac/ryzyka - dopiero po discovery. */
export function createTreeMarkerStyle(fillColor: string): Style {
  return new Style({
    image: new CircleStyle({
      radius: 8,
      fill: new Fill({ color: fillColor }),
      stroke: new Stroke({ color: '#ffffff', width: 2 }),
    }),
  });
}

/** Kolory markerow - stale, bo mapa (podklad OSM) wyglada tak samo w jasnym i ciemnym motywie. */
export const SAVED_TREE_MARKER_COLOR = '#2f6b3c';
export const PENDING_TREE_MARKER_COLOR = '#d9480f';

/** Tolerancja trafienia w marker (px) - wygodniejsze stukanie palcem na tablecie. */
export const MARKER_HIT_TOLERANCE_PX = 8;

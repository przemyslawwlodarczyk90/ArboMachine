import type BaseLayer from 'ol/layer/Base';
import ImageLayer from 'ol/layer/Image';
import TileLayer from 'ol/layer/Tile';
import ImageWMS from 'ol/source/ImageWMS';
import OSM from 'ol/source/OSM';
import TileWMS from 'ol/source/TileWMS';
import type { BasemapId, BasemapSelection } from './basemapPreference';

// Warstwy podkladow (etap 31). Uslugi GUGiK (WMS) sprawdzone 2026-10-20: GetMap w EPSG:3857 (uklad widoku mapy)
// zwraca obraz, wiec nie trzeba przelaczac mapy na PL-1992. Kafle zewnetrzne NIE sa zapisywane offline
// (service worker obsluguje tylko wlasny origin) - patrz docs/DO_DOPRECYZOWANIA.md.

const GEOPORTAL_ORTHOPHOTO_WMS_URL = 'https://mapy.geoportal.gov.pl/wss/service/PZGIK/ORTO/WMS/HighResolution';
const GEOPORTAL_TOPO_WMS_URL = 'https://mapy.geoportal.gov.pl/wss/service/img/guest/TOPO/MapServer/WMSServer';
const PARCELS_WMS_URL = 'https://integracja.gugik.gov.pl/cgi-bin/KrajowaIntegracjaEwidencjiGruntow';
const GUGIK_ATTRIBUTION = 'Dane: © <a href="https://www.geoportal.gov.pl" target="_blank" rel="noreferrer">GUGiK</a>';

/** Dzialki dopiero od przyblizenia "osiedle/ulica" - wyzej to tysiace obrysow i wolne odpowiedzi uslugi. */
const PARCELS_MIN_ZOOM = 15;

const BASEMAP_ID_PROPERTY = 'basemapId';
const PARCELS_OVERLAY_PROPERTY = 'isParcelsOverlay';

function geoportalWmsLayer(url: string, layers: string, basemapId: BasemapId) {
  const layer = new TileLayer({
    visible: false,
    source: new TileWMS({ url, params: { LAYERS: layers, VERSION: '1.3.0', FORMAT: 'image/jpeg' }, attributions: GUGIK_ATTRIBUTION }),
  });
  layer.set(BASEMAP_ID_PROPERTY, basemapId);
  return layer;
}

/**
 * Wszystkie podklady (widoczny tylko wybrany) + nakladka dzialek - kolejnosc = kolejnosc rysowania. Dzialki jako
 * JEDEN obraz na widok (ImageWMS), a nie kafle - przy kaflach usluga podpisuje numer dzialki w kazdym kaflu osobno.
 */
export function createBasemapLayers(): BaseLayer[] {
  const openStreetMap = new TileLayer({ source: new OSM(), visible: false });
  openStreetMap.set(BASEMAP_ID_PROPERTY, 'OSM');
  const parcels = new ImageLayer({
    visible: false,
    minZoom: PARCELS_MIN_ZOOM,
    source: new ImageWMS({
      url: PARCELS_WMS_URL,
      params: { LAYERS: 'dzialki,numery_dzialek', VERSION: '1.3.0', FORMAT: 'image/png', TRANSPARENT: true },
      attributions: 'Działki: © GUGiK (Krajowa Integracja Ewidencji Gruntów)',
    }),
  });
  parcels.set(PARCELS_OVERLAY_PROPERTY, true);
  return [
    openStreetMap,
    geoportalWmsLayer(GEOPORTAL_ORTHOPHOTO_WMS_URL, 'Raster', 'GEOPORTAL_ORTHOPHOTO'),
    geoportalWmsLayer(GEOPORTAL_TOPO_WMS_URL, 'Raster', 'GEOPORTAL_TOPO'),
    parcels,
  ];
}

/** Pokazuje wybrany podklad i (opcjonalnie) dzialki; pozostale warstwy ukryte - nie pobieraja kafli. */
export function applyBasemapSelection(basemapLayers: BaseLayer[], selection: BasemapSelection) {
  for (const layer of basemapLayers) {
    layer.setVisible(layer.get(PARCELS_OVERLAY_PROPERTY)
      ? selection.showParcels
      : layer.get(BASEMAP_ID_PROPERTY) === selection.basemapId);
  }
}

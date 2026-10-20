import { useEffect, useRef } from 'react';
import Feature from 'ol/Feature';
import type Map from 'ol/Map';
import Point from 'ol/geom/Point';
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import {
  createMapWithBasemap, createTreeMarkerStyle, PENDING_TREE_MARKER_COLOR, SAVED_TREE_MARKER_COLOR,
  showDefaultView, TREE_FOCUS_ZOOM,
} from '../../../map/openLayersBaseMap';
import type { TreeLocation } from '../treeDataTypes';
import { toMapCoordinate, toTreeLocation } from './treeLocationMapCoordinates';

/** Ustawia albo usuwa punkt jedynego Feature w warstwie. */
function showPointOrNothing(markerSource: VectorSource<Feature<Point>>, location: TreeLocation | null) {
  markerSource.clear();
  if (location) markerSource.addFeature(new Feature(new Point(toMapCoordinate(location))));
}

/**
 * Mapa lokalizacji jednego drzewa (OpenLayers). Komponent tylko wyswietla i zglasza klikniecia - nie wysyla
 * niczego do API; stan edycji i zapis trzyma TreeLocationSection.
 * - savedLocation: zapisany punkt (z odpowiedzi backendu) - zielony marker; jego zmiana centruje mape.
 * - pendingLocation: tymczasowo wskazany punkt przed zapisem - pomaranczowy marker.
 * - isPickingLocation: tylko wtedy klikniecie mapy wskazuje punkt (onLocationPicked, juz w WGS84).
 * Instancja mapy powstaje raz i jest niszczona przy odmontowaniu.
 */
export function TreeLocationMap({ savedLocation, pendingLocation, isPickingLocation, onLocationPicked }: {
  savedLocation: TreeLocation | null;
  pendingLocation: TreeLocation | null;
  isPickingLocation: boolean;
  onLocationPicked: (pickedLocation: TreeLocation) => void;
}) {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<Map | null>(null);
  const savedMarkerSourceRef = useRef<VectorSource<Feature<Point>> | null>(null);
  const pendingMarkerSourceRef = useRef<VectorSource<Feature<Point>> | null>(null);
  // Listener klikniecia rejestrujemy raz - aktualny tryb i callback czyta z refow.
  const isPickingLocationRef = useRef(isPickingLocation);
  isPickingLocationRef.current = isPickingLocation;
  const onLocationPickedRef = useRef(onLocationPicked);
  onLocationPickedRef.current = onLocationPicked;

  useEffect(() => {
    if (!mapContainerRef.current) return;
    const savedMarkerSource = new VectorSource<Feature<Point>>();
    const pendingMarkerSource = new VectorSource<Feature<Point>>();
    const map = createMapWithBasemap(mapContainerRef.current, [
      new VectorLayer({ source: savedMarkerSource, style: createTreeMarkerStyle(SAVED_TREE_MARKER_COLOR) }),
      // Tymczasowy punkt nad zapisanym - przy wyborze obok siebie widac "bylo" i "bedzie".
      new VectorLayer({ source: pendingMarkerSource, style: createTreeMarkerStyle(PENDING_TREE_MARKER_COLOR) }),
    ]);
    map.on('singleclick', (clickEvent) => {
      if (isPickingLocationRef.current) onLocationPickedRef.current(toTreeLocation(clickEvent.coordinate));
    });

    mapRef.current = map;
    savedMarkerSourceRef.current = savedMarkerSource;
    pendingMarkerSourceRef.current = pendingMarkerSource;
    return () => {
      map.setTarget(undefined);
      map.dispose();
      mapRef.current = null;
      savedMarkerSourceRef.current = null;
      pendingMarkerSourceRef.current = null;
    };
  }, []);

  // Zaleznosc od liczb, a nie od obiektu: nowy obiekt drzewa z tym samym punktem (np. po edycji gatunku)
  // nie przesuwa widoku, ktory uzytkownik mogl ustawic recznie.
  const savedLatitude = savedLocation?.latitude ?? null;
  const savedLongitude = savedLocation?.longitude ?? null;
  useEffect(() => {
    const map = mapRef.current;
    const savedMarkerSource = savedMarkerSourceRef.current;
    if (!map || !savedMarkerSource) return;
    const location = savedLatitude !== null && savedLongitude !== null
      ? { latitude: savedLatitude, longitude: savedLongitude }
      : null;
    showPointOrNothing(savedMarkerSource, location);
    if (location) {
      map.getView().setCenter(toMapCoordinate(location));
      map.getView().setZoom(TREE_FOCUS_ZOOM);
    } else {
      showDefaultView(map);
    }
  }, [savedLatitude, savedLongitude]);

  useEffect(() => {
    if (pendingMarkerSourceRef.current) showPointOrNothing(pendingMarkerSourceRef.current, pendingLocation);
  }, [pendingLocation]);

  return (
    <div ref={mapContainerRef} role="region" aria-label="Mapa lokalizacji drzewa"
         className={`map-container map-container-compact${isPickingLocation ? ' map-container-picking' : ''}`} />
  );
}

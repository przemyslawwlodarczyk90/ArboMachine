// Warstwy referencyjne projektu (etap 31) - odpowiadaja DTO backendu w pakiecie domain.referencelayer.dto.

/** Warstwa bez geometrii (GET /api/projects/{projectId}/reference-layers). */
export interface ReferenceLayer {
  id: number;
  projectId: number;
  name: string;
  sourceFileName: string | null;
  /** GEOJSON, SHAPEFILE, GEOPACKAGE, DXF */
  sourceFormat: string;
  /** np. "EPSG:2178" - uklad pliku zrodlowego */
  sourceCrs: string | null;
  featureCount: number;
  createdAt: string;
}

/** Warstwa gotowa do narysowania: geometria GeoJSON (WGS 84) i kolor przypisany w kolejnosci warstw. */
export interface ReferenceLayerOnMap {
  id: number;
  color: string;
  geoJson: unknown;
}
